package com.hearo.signlanguage.job;

import com.hearo.signlanguage.dto.IngestResultDto;
import com.hearo.signlanguage.service.SignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignIngestScheduler {

    private final SignService signService;

    // 겹치기 방지용(동시에 두 번 돌지 않도록)
    private final ReentrantLock lock = new ReentrantLock();

    // 프로퍼티로 온/오프, 페이지 사이즈, 병렬/순차 선택 가능하게
    @Value("${sign.ingest.enabled:true}")
    private boolean enabled;

    @Value("${sign.ingest.page-size:500}")
    private int pageSize;

    // 기본은 순차(429 리스크 낮춤). 필요하면 true로.
    @Value("${sign.ingest.parallel:false}")
    private boolean useParallel;


    // 스케줄러 -> 매일 밤 자정 실행
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void nightlyIngest() {
        if (!enabled) {
            log.info("[SignIngestScheduler] disabled by property. skip.");
            return;
        }

        if (!lock.tryLock()) {
            log.info("[SignIngestScheduler] another job is running. skip.");
            return;
        }

        try {
            log.info("[SignIngestScheduler] start. pageSize={}, mode={}",
                    pageSize, useParallel ? "PARALLEL" : "SEQUENTIAL");

            IngestResultDto result = useParallel
                    ? signService.ingestAllParallel(pageSize)
                    : signService.ingestAll(pageSize);

            log.info("[SignIngestScheduler] done. fetched~={}, pageSize={}",
                    result.getTotalFetched(), result.getPageSize());

        } catch (Exception e) {
            log.warn("[SignIngestScheduler] failed: {}", e.toString(), e);
        } finally {
            lock.unlock();
        }
    }
}
