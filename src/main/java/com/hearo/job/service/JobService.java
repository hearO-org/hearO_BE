package com.hearo.job.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hearo.job.client.JobApiClient;
import com.hearo.job.client.dto.JobRawResponse;
import com.hearo.job.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 구인정보 비즈니스 로직
 * - 실시간 조회, 서버사이드 필터, 상세 캐시, 재시도/백오프
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobApiClient client;

    // rno → 상세 캐시
    private final Cache<String, JobDetailDto> detailCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();

    @Value("${job.api.default-num-of-rows:100}")
    private int defaultFetchRows;

    /** 외부 페이지 그대로 프록시 */
    public JobPageDto externalList(int pageNo, int numOfRows) {
        JobRawResponse raw = fetchWithRetry(pageNo, numOfRows, 6, 600);
        validateHeader(raw);
        return toPageDto(raw, true);
    }

    /** 서버사이드 필터 검색 (지역 포함) */
    public JobPageDto externalFiltered(JobFilter filter, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;

        final int MAX_SCAN_PAGES = 25;                      // 남용 방지
        final int fetchRows = Math.max(defaultFetchRows, size);

        List<JobDetailDto> bucket = new ArrayList<>();
        Predicate<JobDetailDto> predicate = buildPredicate(filter);

        int needOffset = (page - 1) * size;
        int needTotal  = needOffset + size;

        int scanned = 0;
        int pageNo = 1;

        while (bucket.size() < needTotal && scanned < MAX_SCAN_PAGES) {
            JobRawResponse raw = fetchWithRetry(pageNo, fetchRows, 4, 400);
            var items = raw.getBody() != null && raw.getBody().getItems() != null
                    ? raw.getBody().getItems().getItem() : List.<JobRawResponse.Item>of();
            if (items.isEmpty()) break;

            for (JobRawResponse.Item i : items) {
                JobItemDto item = mapToItem(i);
                JobDetailDto d  = toDetail(item, i); // rno/rnum 포함
                if (predicate.test(d)) {
                    bucket.add(d);
                    String key = !nvl(i.getRno()).isBlank() ? i.getRno() : nvl(i.getRnum());
                    if (!key.isBlank()) detailCache.put(key, d);
                }
            }
            pageNo++; scanned++;
        }

        List<JobItemDto> pageItems = bucket.stream()
                .skip(needOffset).limit(size)
                .map(JobService::toItem)
                .collect(Collectors.toList());

        return JobPageDto.builder()
                .pageNo(page)
                .numOfRows(size)
                .totalCount(bucket.size()) // 필터 후 총량
                .items(pageItems)
                .build();
    }

    /** 상세 조회 (rno) - 캐시 우선, 미스 시 제한 스캔 */
    public JobDetailDto getDetailByRno(String rno) {
        if (rno == null || rno.isBlank()) throw new IllegalArgumentException("유효한 rno가 필요합니다.");
        JobDetailDto cached = detailCache.getIfPresent(rno);
        if (cached != null) return cached;

        final int MAX_SCAN_PAGES = 15;
        int pageNo = 1;

        while (pageNo <= MAX_SCAN_PAGES) {
            JobRawResponse raw = fetchWithRetry(pageNo, defaultFetchRows, 3, 300);
            var items = raw.getBody() != null && raw.getBody().getItems() != null
                    ? raw.getBody().getItems().getItem() : List.<JobRawResponse.Item>of();
            if (items.isEmpty()) break;

            for (JobRawResponse.Item i : items) {
                if (rno.equals(nvl(i.getRno())) || rno.equals(nvl(i.getRnum()))) {
                    JobDetailDto d = toDetail(mapToItem(i), i);
                    detailCache.put(rno, d);
                    return d;
                }
            }
            pageNo++;
        }
        throw new IllegalArgumentException("해당 rno 항목을 찾을 수 없습니다. rno=" + rno);
    }

    // ---------------- 내부 헬퍼 ----------------

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
        if (!"0000".equals(code) && !"00".equals(code)) {
            String msg = raw.getHeader().getResultMsg();
            throw new IllegalStateException("Job API 오류: " + (msg == null ? code : msg));
        }
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

    private JobPageDto toPageDto(JobRawResponse raw, boolean cacheDetail) {
        var body = raw.getBody();
        var items = (body != null && body.getItems() != null && body.getItems().getItem() != null)
                ? body.getItems().getItem() : List.<JobRawResponse.Item>of();

        List<JobItemDto> list = items.stream().map(JobService::mapToItem).toList();

        if (cacheDetail) {
            for (int idx = 0; idx < items.size(); idx++) {
                var rawItem = items.get(idx);
                var d = toDetail(list.get(idx), rawItem);
                String key = !nvl(rawItem.getRno()).isBlank() ? rawItem.getRno() : nvl(rawItem.getRnum());
                if (!key.isBlank()) detailCache.put(key, d);
            }
        }

        int pageNo = nz(body != null ? body.getPageNo() : null, 1);
        int numOfRows = nz(body != null ? body.getNumOfRows() : null, list.size());
        int totalCount = nz(body != null ? body.getTotalCount() : null, list.size());

        return JobPageDto.builder()
                .items(list)
                .pageNo(pageNo)
                .numOfRows(numOfRows)
                .totalCount(totalCount)
                .build();
    }

    /** 지역/키워드/환경 등 복합 필터 */
    private Predicate<JobDetailDto> buildPredicate(JobFilter f) {
        return d -> {
            if (f == null || !f.hasAny()) return true;

            if (!containsAny(d.getJobNm(), d.getBusplaName(), d.getCompAddr(), f.getKeyword())) return false;
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

    // -------- utils --------
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

    private static String nvl(String s) { return s == null ? "" : s; }
    private static int nz(Integer v, int def) { return v == null ? def : v; }

    private static boolean equalsIfSet(String left, String expect) {
        if (expect == null || expect.isBlank()) return true;
        return nvl(left).equalsIgnoreCase(expect.trim());
    }
    private static boolean containsAny(String a, String b, String c, String term) {
        if (term == null || term.isBlank()) return true;
        String t = term.toLowerCase().trim();
        return nvl(a).toLowerCase().contains(t) || nvl(b).toLowerCase().contains(t) || nvl(c).toLowerCase().contains(t);
    }
    /** 지역 포함 매칭: 공백으로 분할된 토큰을 모두 포함하면 true */
    private static boolean containsRegion(String compAddr, String region) {
        if (region == null || region.isBlank()) return true;
        String addr = nvl(compAddr).toLowerCase();
        String[] tokens = region.trim().toLowerCase().split("\\s+");
        for (String t : tokens) {
            if (!t.isBlank() && !addr.contains(t)) return false;
        }
        return true;
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
}
