package com.hearo.job.service;

import com.hearo.job.client.JobApiClient;
import com.hearo.job.client.dto.JobRawResponse;
import com.hearo.job.dto.JobDetailDto;
import com.hearo.job.dto.JobFilter;
import com.hearo.job.dto.JobItemDto;
import com.hearo.job.dto.JobPageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 구인정보 비즈니스 로직 (Spring Cache + Caffeine 적용)
 * - 실시간 조회 + 서버사이드 필터 + 상세 캐시 + 재시도/백오프
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobApiClient client;

    @Value("${job.api.default-num-of-rows:100}")
    private int defaultFetchRows;

    /** 1) 외부 페이지 프록시 */
    public JobPageDto externalList(int pageNo, int numOfRows) {
        JobRawResponse raw = fetchWithRetry(pageNo, numOfRows, 6, 600);
        validateHeader(raw);
        return toPageDto(raw);
    }

    /** 2) 서버사이드 필터 (region 포함) */
    public JobPageDto externalFiltered(JobFilter filter, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;

        final int MAX_SCAN_PAGES = 25; // 남용 방지
        final int fetchRows = Math.max(defaultFetchRows, size);

        List<JobDetailDto> bucket = new ArrayList<>();
        Predicate<JobDetailDto> predicate = buildPredicate(filter);

        int needOffset = (page - 1) * size;
        int needTotal  = needOffset + size;

        int scanned = 0;
        int pageNo  = 1;

        while (bucket.size() < needTotal && scanned < MAX_SCAN_PAGES) {
            JobRawResponse raw = fetchWithRetry(pageNo, fetchRows, 4, 400);
            validateHeader(raw);

            // 항상 null-safe: item 리스트가 null이어도 빈 리스트로 처리
            List<JobRawResponse.Item> items = safeItems(raw);
            if (items.isEmpty()) break;

            for (JobRawResponse.Item i : items) {
                JobItemDto item = mapToItem(i);
                JobDetailDto d  = toDetail(item, i); // 원시값 합성
                if (predicate.test(d)) bucket.add(d);
            }

            pageNo++;
            scanned++;
        }

        List<JobItemDto> pageItems = bucket.stream()
                .skip(needOffset).limit(size)
                .map(JobService::toItem)
                .collect(Collectors.toList());

        return JobPageDto.builder()
                .pageNo(page)
                .numOfRows(size)
                .totalCount(bucket.size())
                .items(pageItems)
                .build();
    }

    /** 3) 상세 (rno 기준) — Spring Cache 적용 */
    @Cacheable(value = "jobDetail", key = "#rno")
    public JobDetailDto getDetailByRno(String rno) {
        if (rno == null || rno.isBlank())
            throw new IllegalArgumentException("유효한 rno가 필요합니다.");

        // 외부 API는 "상세" 엔드포인트가 없으므로 제한 스캔으로 조회
        final int MAX_SCAN_PAGES = 15;
        int pageNo = 1;

        while (pageNo <= MAX_SCAN_PAGES) {
            JobRawResponse raw = fetchWithRetry(pageNo, defaultFetchRows, 3, 300);
            validateHeader(raw);

            // null-safe
            List<JobRawResponse.Item> items = safeItems(raw);
            if (items.isEmpty()) break;

            for (JobRawResponse.Item i : items) {
                // rno 또는 rnum 일치 시 상세 구성 후 반환
                if (rno.equals(nvl(i.getRno())) || rno.equals(nvl(i.getRnum()))) {
                    return toDetail(mapToItem(i), i);
                }
            }
            pageNo++;
        }

        throw new IllegalArgumentException("해당 구인정보를 찾을 수 없습니다. rno=" + rno);
    }

    /** 캐시 비우기 — 관리용 */
    @CacheEvict(value = "jobDetail", allEntries = true)
    public void evictAllDetailCache() {
        // no-op (AOP 프록시가 캐시를 비움)
    }

    /* ================= 내부 헬퍼 ================= */

    private JobRawResponse fetchWithRetry(int pageNo, int numOfRows, int maxRetry, long baseBackoffMs) {
        int attempt = 0;
        while (true) {
            try {
                return client.fetch(pageNo, numOfRows);
            } catch (RuntimeException ex) {
                attempt++;
                if (attempt > maxRetry || !isTransient(ex)) throw ex;
                long sleep = (long) (baseBackoffMs * Math.pow(1.6, attempt - 1));
                if (isTooManyRequests(ex)) sleep += 2000L;
                log.warn("[JobService] transient err={}, attempt={}, sleep={}ms", rootName(ex), attempt, sleep);
                try { Thread.sleep(sleep); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw ex; }
            }
        }
    }

    private void validateHeader(JobRawResponse raw) {
        if (raw == null || raw.getHeader() == null)
            throw new IllegalStateException("Job API 응답 파싱 실패(header 없음)");
        String code = raw.getHeader().getResultCode();
        if (!Objects.equals(code, "0000") && !Objects.equals(code, "00")) {
            String msg = raw.getHeader().getResultMsg();
            throw new IllegalStateException("Job API 오류: " + (msg == null ? code : msg));
        }
    }

    /**
     * 공통 null-safe item 리스트 추출
     * - body == null → 빈 리스트
     * - body.items == null → 빈 리스트
     * - body.items.item == null → 빈 리스트
     */
    private static List<JobRawResponse.Item> safeItems(JobRawResponse raw) {
        if (raw == null) return List.of();
        var body = raw.getBody();
        if (body == null || body.getItems() == null || body.getItems().getItem() == null) {
            return List.of();
        }
        return body.getItems().getItem();
    }

    private static JobPageDto toPageDto(JobRawResponse raw) {
        var body = (raw != null) ? raw.getBody() : null;
        List<JobRawResponse.Item> items = safeItems(raw);

        List<JobItemDto> list = items.stream().map(JobService::mapToItem).toList();
        int pageNo     = nz(body != null ? body.getPageNo()     : null, 1);
        int numOfRows  = nz(body != null ? body.getNumOfRows()  : null, list.size());
        int totalCount = nz(body != null ? body.getTotalCount() : null, list.size());

        return JobPageDto.builder()
                .items(list)
                .pageNo(pageNo)
                .numOfRows(numOfRows)
                .totalCount(totalCount)
                .build();
    }

    private static JobItemDto mapToItem(JobRawResponse.Item i) {
        return JobItemDto.builder()
                .rno(nvl(i.getRno()))
                .jobNm(nvl(i.getJobNm()))
                .busplaName(nvl(i.getBusplaName()))
                .compAddr(nvl(i.getCompAddr()))
                .cntctNo(nvl(i.getCntctNo()))
                .empType(nvl(i.getEmpType()))
                .enterType(nvl(i.getEnterType()))
                .termDate(nvl(i.getTermDate()))
                .salary(nvl(i.getSalary()))
                .salaryType(nvl(i.getSalaryType()))
                .reqCareer(nvl(i.getReqCareer()))
                .reqEduc(nvl(i.getReqEduc()))
                .regagnName(nvl(i.getRegagnName()))
                .offerregDt(nvl(i.getOfferregDt()))
                .regDt(nvl(i.getRegDt()))
                .build();
    }

    private static JobDetailDto toDetail(JobItemDto i, JobRawResponse.Item raw) {
        return JobDetailDto.builder()
                .rno(nvl(raw.getRno()))
                .rnum(nvl(raw.getRnum()))
                .jobNm(i.getJobNm())
                .busplaName(i.getBusplaName())
                .compAddr(i.getCompAddr())
                .cntctNo(i.getCntctNo())
                .empType(i.getEmpType())
                .enterType(i.getEnterType())
                .termDate(i.getTermDate())
                .salary(i.getSalary())
                .salaryType(i.getSalaryType())
                .reqCareer(i.getReqCareer())
                .reqEduc(i.getReqEduc())
                .regagnName(i.getRegagnName())
                .offerregDt(i.getOfferregDt())
                .regDt(i.getRegDt())
                // 작업환경 세부
                .envBothHands(nvl(raw.getEnvBothHands()))
                .envEyesight(nvl(raw.getEnvEyesight()))
                .envHandwork(nvl(raw.getEnvHandwork()))
                .envLiftPower(nvl(raw.getEnvLiftPower()))
                .envLstnTalk(nvl(raw.getEnvLstnTalk()))
                .envStndWalk(nvl(raw.getEnvStndWalk()))
                .build();
    }

    private static JobItemDto toItem(JobDetailDto d) {
        return JobItemDto.builder()
                .rno(d.getRno())
                .jobNm(d.getJobNm())
                .busplaName(d.getBusplaName())
                .compAddr(d.getCompAddr())
                .cntctNo(d.getCntctNo())
                .empType(d.getEmpType())
                .enterType(d.getEnterType())
                .termDate(d.getTermDate())
                .salary(d.getSalary())
                .salaryType(d.getSalaryType())
                .reqCareer(d.getReqCareer())
                .reqEduc(d.getReqEduc())
                .regagnName(d.getRegagnName())
                .offerregDt(d.getOfferregDt())
                .regDt(d.getRegDt())
                .build();
    }

    /* --------- 공통 유틸 --------- */
    private static String nvl(String s) { return s == null ? "" : s; }

    private static int nz(Integer v, int def) { return v == null ? def : v; }

    private static boolean isTransient(Throwable t) {
        if (t instanceof WebClientRequestException) return true;
        if (t instanceof WebClientResponseException w) {
            int s = w.getStatusCode().value();
            return s == 429 || s == 502 || s == 503 || s == 504;
        }
        Throwable c = t.getCause();
        while (c != null) {
            if (c instanceof java.net.SocketTimeoutException
                    || c instanceof io.netty.handler.timeout.ReadTimeoutException) return true;
            c = c.getCause();
        }
        return false;
    }

    private static boolean isTooManyRequests(Throwable t) {
        return (t instanceof WebClientResponseException w) && w.getStatusCode().value() == 429;
    }

    private static String rootName(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        return c.getClass().getSimpleName();
    }

    /** 지역 포함 매칭: 공백으로 분할된 토큰을 모두 주소에 포함하면 true */
    private static boolean containsRegion(String compAddr, String region) {
        if (region == null || region.isBlank()) return true;
        String addr = nvl(compAddr).toLowerCase();
        String[] tokens = region.trim().toLowerCase().split("\\s+");
        for (String t : tokens) {
            if (!t.isBlank() && !addr.contains(t)) return false;
        }
        return true;
    }

    private static boolean equalsIfSet(String left, String expect) {
        if (expect == null || expect.isBlank()) return true;
        return nvl(left).equalsIgnoreCase(expect.trim());
    }

    private Predicate<JobDetailDto> buildPredicate(JobFilter f) {
        return d -> {
            if (f == null) return true;

            // keyword: jobNm / busplaName / compAddr 에 포함
            if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
                String t = f.getKeyword().trim().toLowerCase();
                boolean hit = nvl(d.getJobNm()).toLowerCase().contains(t)
                        || nvl(d.getBusplaName()).toLowerCase().contains(t)
                        || nvl(d.getCompAddr()).toLowerCase().contains(t);
                if (!hit) return false;
            }

            if (!containsRegion(d.getCompAddr(), f.getRegion())) return false;

            if (!equalsIfSet(d.getEmpType(), f.getEmpType())) return false;
            if (!equalsIfSet(d.getSalaryType(), f.getSalaryType())) return false;
            if (!equalsIfSet(d.getEnterType(), f.getEnterType())) return false;

            if (!equalsIfSet(d.getEnvBothHands(), f.getEnvBothHands())) return false;
            if (!equalsIfSet(d.getEnvEyesight(), f.getEnvEyesight())) return false;
            if (!equalsIfSet(d.getEnvHandwork(), f.getEnvHandwork())) return false;
            if (!equalsIfSet(d.getEnvLiftPower(), f.getEnvLiftPower())) return false;
            if (!equalsIfSet(d.getEnvLstnTalk(), f.getEnvLstnTalk())) return false;
            if (!equalsIfSet(d.getEnvStndWalk(), f.getEnvStndWalk())) return false;

            return true;
        };
    }
}
