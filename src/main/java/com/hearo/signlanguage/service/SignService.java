package com.hearo.signlanguage.service;

import com.hearo.signlanguage.client.SignApiClient;
import com.hearo.signlanguage.client.dto.SignRawResponse;
import com.hearo.signlanguage.domain.SignEntry;
import com.hearo.signlanguage.dto.IngestResultDto;
import com.hearo.signlanguage.dto.SignItemDto;
import com.hearo.signlanguage.dto.SignPageDto;
import com.hearo.signlanguage.repository.SignEntryBulkRepository;
import com.hearo.signlanguage.repository.SignEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignService {

    private final SignApiClient client;
    private final SignEntryRepository repo;
    private final SignEntryBulkRepository bulk;

    public SignPageDto externalList(int pageNo, int numOfRows) {
        SignRawResponse raw = client.fetch(null, pageNo, numOfRows);
        return toPageDto(raw);
    }

    public SignPageDto externalSearch(String keyword, int pageNo, int numOfRows) {
        SignRawResponse raw = client.fetch(keyword, pageNo, numOfRows);
        return toPageDto(raw);
    }

    public Page<SignEntry> listFromDb(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by("id").descending());
        return repo.findAll(pageable);
    }

    public Page<SignEntry> searchFromDb(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by("id").descending());
        return repo.findByTitleContaining(keyword, pageable);
    }

    /** ========== 순차 ingest ========== */
    @Transactional
    public IngestResultDto ingestAll(int pageSize) {
        int pageNo = 1;
        int totalCount = Integer.MAX_VALUE;
        int totalFetched = 0;

        log.info("Sign ingest start pageSize={}", pageSize);

        while (true) {
            SignPageDto page;

            try {
                page = fetchPageWithRetry(pageNo, pageSize, 2, 700);
            } catch (Exception e) {
                log.warn("Sign ingest skip pageNo={} (reason={})", pageNo, rootName(e));
                pageNo++;
                continue;
            }

            if (pageNo == 1) totalCount = page.getTotalCount();
            if (page.getItems().isEmpty()) break;

            int[] res = bulk.upsertBatch(page.getItems());
            int affected = Arrays.stream(res).sum();

            log.info("Sign ingest page done pageNo={} affectedRows~={}",
                    pageNo, affected);

            totalFetched += page.getItems().size();
            if (totalFetched >= totalCount) break;
            pageNo++;
        }

        log.info("Sign ingest done totalCount={} totalFetched={}",
                totalCount, totalFetched);

        return new IngestResultDto(totalCount, totalFetched, 0, 0, pageSize);
    }

    /** ========== 병렬 ingest ========== */
    @Transactional
    public IngestResultDto ingestAllParallel(int pageSize) {
        int totalCount;
        int totalFetched = 0;

        log.info("Sign ingest (parallel) start pageSize={}", pageSize);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<SignPageDto>> futures = new ArrayList<>();

        // 1페이지 먼저 호출 → totalCount 계산
        SignPageDto firstPage = fetchPageWithRetry(1, pageSize, 2, 700);
        totalCount = firstPage.getTotalCount();
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        // 1페이지 먼저 저장
        bulk.upsertBatch(firstPage.getItems());
        totalFetched += firstPage.getItems().size();

        // 2페이지 이후 병렬 제출
        for (int pageNo = 2; pageNo <= totalPages; pageNo++) {
            final int p = pageNo;
            futures.add(executor.submit(() -> {
                try {
                    return fetchPageWithRetry(p, pageSize, 2, 700);
                } catch (Exception e) {
                    log.warn("skip pageNo={} (reason={})", p, rootName(e));
                    return null;
                }
            }));
        }

        for (Future<SignPageDto> f : futures) {
            try {
                SignPageDto page = f.get();
                if (page == null || page.getItems().isEmpty()) continue;

                int[] res = bulk.upsertBatch(page.getItems());
                int affected = Arrays.stream(res).sum();
                totalFetched += page.getItems().size();

                log.info("pageNo={} done affectedRows~={}", page.getPageNo(), affected);
            } catch (Exception e) {
                log.warn("future error: {}", e.toString());
            }
        }

        executor.shutdown();

        log.info("Sign ingest parallel done totalCount={} totalFetched={}",
                totalCount, totalFetched);

        return new IngestResultDto(totalCount, totalFetched, 0, 0, pageSize);
    }

    // ===== 내부 헬퍼 =====
    private SignPageDto fetchPage(int pageNo, int pageSize) {
        SignRawResponse raw = client.fetch(null, pageNo, pageSize);
        if (raw == null || raw.getResponse() == null || raw.getResponse().getHeader() == null) {
            throw new IllegalStateException("외부 API 응답 파싱 실패");
        }
        var header = raw.getResponse().getHeader();
        if (!"0000".equals(header.getResultCode())) {
            String msg = (header.getResultMsg() != null) ? header.getResultMsg() : "외부 API 오류";
            throw new IllegalStateException("KCISA API 오류: " + msg);
        }
        var body = raw.getResponse().getBody();

        List<SignRawResponse.Item> items =
                (body != null && body.getItems() != null && body.getItems().getItem() != null)
                        ? body.getItems().getItem() : List.of();

        List<SignItemDto> list = items.stream().map(this::mapToItemDto).toList();

        String pageNoStr     = (body != null) ? body.getPageNo()     : null;
        String numOfRowsStr  = (body != null) ? body.getNumOfRows()  : null;
        String totalCountStr = (body != null) ? body.getTotalCount() : null;

        return SignPageDto.builder()
                .items(list)
                .pageNo(parseIntDefault(pageNoStr, 1))
                .numOfRows(parseIntDefault(numOfRowsStr, list.size()))
                .totalCount(parseIntDefault(totalCountStr, list.size()))
                .build();
    }

    private SignPageDto fetchPageWithRetry(int pageNo, int pageSize, int maxRetry, long backoffMs) {
        int attempt = 0;
        while (true) {
            try {
                return fetchPage(pageNo, pageSize);
            } catch (RuntimeException ex) {
                attempt++;
                if (attempt > maxRetry || !isTransient(ex)) {
                    throw ex;
                }
                try { Thread.sleep(backoffMs); } catch (InterruptedException ignored) {}
            }
        }
    }

    private boolean isTransient(Throwable t) {
        if (t instanceof WebClientRequestException) return true;
        Throwable c = t.getCause();
        while (c != null) {
            if (c instanceof java.net.SocketTimeoutException
                    || c instanceof io.netty.handler.timeout.ReadTimeoutException) return true;
            c = c.getCause();
        }
        return false;
    }

    private String rootName(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        return c.getClass().getSimpleName();
    }

    private SignPageDto toPageDto(SignRawResponse raw) {
        var body = raw.getResponse().getBody();
        var itemsRaw = (body != null && body.getItems() != null)
                ? body.getItems().getItem()
                : List.<SignRawResponse.Item>of();

        List<SignItemDto> list = itemsRaw.stream()
                .map(this::mapToItemDto)
                .toList();

        int pageNo     = parseIntDefault(body != null ? body.getPageNo() : null, 1);
        int totalCount = parseIntDefault(body != null ? body.getTotalCount() : null, list.size());

        return SignPageDto.builder()
                .items(list)
                .pageNo(pageNo)
                .numOfRows(list.size())
                .totalCount(totalCount)
                .build();
    }

    private SignItemDto mapToItemDto(SignRawResponse.Item i) {
        return SignItemDto.builder()
                .title(nvl(i.getTitle()))
                .localId(nvl(i.getLocalId()))
                .videoUrl(nvl(i.getSubDescription()))
                .thumbnailUrl(nvl(i.getImageObject()))
                .signDescription(nvl(i.getSignDescription()))
                .images(splitCsv(i.getSignImages()))
                .sourceUrl(nvl(i.getUrl()))
                .collectionDb(nvl(i.getCollectionDb()))
                .categoryType(nvl(i.getCategoryType()))
                .viewCount(parseIntOrNull(i.getViewCount()))
                .build();
    }

    private static String nvl(String s) { return (s == null) ? "" : s; }
    private static int parseIntDefault(String s, int def) {
        try { return (s == null) ? def : Integer.parseInt(s); }
        catch (NumberFormatException e) { return def; }
    }
    private static Integer parseIntOrNull(String s) {
        try { return (s == null) ? null : Integer.parseInt(s); }
        catch (NumberFormatException e) { return null; }
    }
    private static List<String> splitCsv(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(x -> !x.isBlank())
                .toList();
    }
}
