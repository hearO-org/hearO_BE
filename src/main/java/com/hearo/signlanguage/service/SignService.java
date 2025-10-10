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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignService {

    private final SignApiClient client;
    private final SignEntryRepository repo;
    private final SignEntryBulkRepository bulkRepo;

    // ===== 조회 =====

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

    // ===== 수집(순차) : totalCount 무시, 빈 페이지 만나면 종료 =====

    @Transactional
    public IngestResultDto ingestAll(int pageSize) {
        int pageNo = 1;
        int totalFetched = 0;

        log.info("Sign ingest start pageSize={}", pageSize);

        while (true) {
            SignPageDto page;
            try {
                page = fetchPageWithRetry(pageNo, pageSize, 6, 600);
            } catch (RuntimeException e) {
                log.warn("Sign ingest skip pageNo={} (reason={})", pageNo, rootName(e));
                pageNo++;
                continue;
            }

            if (page.getItems().isEmpty()) {
                log.info("Sign ingest reached empty page. stop at pageNo={}", pageNo);
                break;
            }

            int[] res = bulkRepo.upsertBatch(page.getItems());
            totalFetched += page.getItems().size();
            log.info("Sign ingest page done pageNo={} items={} affectedRows~={}",
                    pageNo, page.getItems().size(), res.length);

            pageNo++;
        }

        log.info("Sign ingest done totalFetched={}", totalFetched);
        return new IngestResultDto(-1, totalFetched, -1, -1, pageSize);
    }

    // ===== 수집(병렬) : for-loop 워커 + 429/Quota 즉시 종료 + 빈 페이지 연속 종료 =====

    @Transactional
    public IngestResultDto ingestAllParallel(int pageSize) {
        final int WORKERS = 1;          // 동시 API 호출 수 (권장 1~2; 높은 값은 429 유발)
        final int STOP_AFTER_EMPTY = 8; // 빈 페이지 연속 N회면 종료
        final int START_PAGE = 1;

        AtomicInteger nextPage = new AtomicInteger(START_PAGE);
        AtomicInteger totalFetched = new AtomicInteger(0);
        AtomicInteger emptyStreak = new AtomicInteger(0);
        AtomicBoolean stopAll = new AtomicBoolean(false);

        log.info("Sign ingest (parallel) start pageSize={} workers={}", pageSize, WORKERS);

        ExecutorService pool = Executors.newFixedThreadPool(WORKERS);
        CopyOnWriteArrayList<Future<?>> futures = new CopyOnWriteArrayList<>();

        for (int workerIdx = 0; workerIdx < WORKERS; workerIdx++) {
            final int idx = workerIdx;
            Future<?> future = pool.submit(() -> {
                while (!stopAll.get() && emptyStreak.get() < STOP_AFTER_EMPTY) {
                    int pageNo = nextPage.getAndIncrement();
                    SignPageDto page;

                    try {
                        // 429(쿼터 초과) 시 재시도 의미 없으니 retry 0
                        page = fetchPageWithRetry(pageNo, pageSize, 0, 0);
                    } catch (RuntimeException e) {
                        if (isDailyQuotaExceeded(e) || isTooManyRequests(e)) {
                            stopAll.set(true);
                            log.warn("Worker={} quota exceeded/429 at pageNo={}, stop all.",
                                    idx, pageNo);
                            break;
                        }
                        if (isTransient(e)) {
                            log.warn("Worker={} transient error pageNo={} (reason={}), skip.",
                                    idx, pageNo, rootName(e));
                            continue;
                        }
                        log.warn("Worker={} non-retryable error pageNo={} (reason={}), skip.",
                                idx, pageNo, rootName(e));
                        continue;
                    }

                    if (page.getItems().isEmpty()) {
                        int streak = emptyStreak.incrementAndGet();
                        log.info("Worker={} got empty page pageNo={}, emptyStreak={}/{}",
                                idx, pageNo, streak, STOP_AFTER_EMPTY);
                        continue;
                    } else {
                        emptyStreak.set(0);
                    }

                    int[] res = bulkRepo.upsertBatch(page.getItems());
                    totalFetched.addAndGet(page.getItems().size());

                    log.info("Worker={} pageNo={} done, items={}, affectedRows~={}",
                            idx, pageNo, page.getItems().size(), res.length);
                }
            });
            futures.add(future);
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ee) {
                log.warn("Parallel ingest worker error: {}", rootName(ee));
            }
        }

        pool.shutdownNow();

        log.info("Sign ingest (parallel) done totalFetched~={}", totalFetched.get());
        return new IngestResultDto(-1, totalFetched.get(), -1, -1, pageSize);
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

        String pageNoStr    = (body != null) ? body.getPageNo()    : null;
        String numOfRowsStr = (body != null) ? body.getNumOfRows() : null;

        // totalCount는 신뢰하지 않고 실제 개수로 채운다
        return SignPageDto.builder()
                .items(list)
                .pageNo(parseIntDefault(pageNoStr, 1))
                .numOfRows(parseIntDefault(numOfRowsStr, list.size()))
                .totalCount(list.size())
                .build();
    }

    private SignPageDto fetchPageWithRetry(int pageNo, int pageSize, int maxRetry, long baseBackoffMs) {
        int attempt = 0;
        while (true) {
            try {
                return fetchPage(pageNo, pageSize);
            } catch (RuntimeException ex) {
                attempt++;
                if (attempt > maxRetry || !isTransient(ex)) {
                    throw ex;
                }
                long sleep = (long) (baseBackoffMs * Math.pow(1.6, attempt - 1));
                if (isTooManyRequests(ex)) sleep += 2000L; // 429면 더 쉰다
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
    }

    private boolean isTransient(Throwable t) {
        if (t instanceof WebClientRequestException) return true; // 네트워크 이슈
        if (t instanceof WebClientResponseException wex) {
            int s = wex.getStatusCode().value();
            // 429, 5xx는 일시 오류로 간주(필요 시 정책 조절)
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

    private boolean isTooManyRequests(Throwable t) {
        if (t instanceof WebClientResponseException wex) {
            return wex.getStatusCode().value() == 429;
        }
        return false;
    }

    private boolean isDailyQuotaExceeded(Throwable t) {
        if (t instanceof WebClientResponseException wex) {
            try {
                String body = wex.getResponseBodyAsString();
                if (body != null) {
                    String lower = body.toLowerCase();
                    // 예: {"message":"Quota exceeded ! You reach the limit of 1000 requests per 1 days","http_status_code":429}
                    return lower.contains("quota exceeded");
                }
            } catch (Exception ignored) { }
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

        int pageNo = parseIntDefault(body != null ? body.getPageNo() : null, 1);

        return SignPageDto.builder()
                .items(list)
                .pageNo(pageNo)
                .numOfRows(list.size())
                .totalCount(list.size())
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