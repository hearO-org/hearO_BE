package com.hearo.sound.service;

import com.hearo.alert.service.NotificationService;
import com.hearo.sound.client.SoundAiClient;
import com.hearo.sound.config.SoundAiConfig;
import com.hearo.sound.domain.SoundDetectLog;
import com.hearo.sound.dto.AiInferResponse;
import com.hearo.sound.dto.DetectSoundResponse;
import com.hearo.sound.dto.SoundDetectLogRes;
import com.hearo.sound.repository.SoundDetectLogRepository;
import com.hearo.user.domain.User;
import com.hearo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SoundService {

    private final SoundAiClient aiClient;
    private final SoundAiConfig config;
    private final NotificationService notificationService;
    private final SoundDetectLogRepository soundDetectLogRepository;
    private final UserService userService;

    /**
     * @param wavBytes  1~2초짜리 WAV 바이너리
     * @param filename  원본 파일 이름
     * @param userId    현재 사용자 ID (알림 보낼 대상)
     */
    public DetectSoundResponse detect(byte[] wavBytes, String filename, Long userId) {

        long start = System.currentTimeMillis();
        long fileSize = wavBytes != null ? wavBytes.length : 0L;

        // 로그인 필수이므로 userId는 항상 존재
        User user = userService.getById(userId);

        // 1) AI 서버 호출 (예외 안전하게 처리)
        AiInferResponse aiRes;
        try {
            aiRes = aiClient.infer(wavBytes, filename);
        } catch (Exception e) {
            // AI 서버 연결 실패 / 타임아웃 등
            long end = System.currentTimeMillis();
            long took = end - start;

            log.error("[SoundService] AI infer failed. filename={}", filename, e);

            // 실패 로그 저장
            SoundDetectLog failLog = SoundDetectLog.failLog(
                    user,
                    filename,
                    fileSize,
                    "[AI 호출 실패] " + e.getMessage(),
                    took
            );
            soundDetectLogRepository.save(failLog);

            // 서비스는 graceful하게 unknown 리턴
            return new DetectSoundResponse("unknown", 0.0, false, Map.of());
        }

        // 2) 응답이 비었을 때 fallback
        if (aiRes == null || CollectionUtils.isEmpty(aiRes.probs())) {
            long end = System.currentTimeMillis();
            long took = end - start;

            log.warn("[SoundService] AI response empty. filename={}", filename);

            // 실패 로그 저장
            SoundDetectLog emptyLog = SoundDetectLog.failLog(
                    user,
                    filename,
                    fileSize,
                    "[AI 응답 없음] probs가 비어 있음",
                    took
            );
            soundDetectLogRepository.save(emptyLog);

            return new DetectSoundResponse("unknown", 0.0, false, Map.of());
        }

        // 3) AI 확률값에서 가장 가능성이 높은 라벨 선택
        Map<String, Double> probs = aiRes.probs();
        Map.Entry<String, Double> maxEntry = probs.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .orElseThrow(() ->
                        new IllegalStateException("probs is not empty but maxEntry not found")
                );

        String label = maxEntry.getKey();
        double maxProb = maxEntry.getValue();

        // 4) 알림 여부 결정
        boolean alert = maxProb >= config.getMinAlertProb()
                && isDangerousLabel(label);

        // 5) 위험 소리 감지 시 알림 전송 (실패해도 detect 결과는 정상 리턴)
        if (alert) {
            try {
                notificationService.sendDangerAlert(userId, label, maxProb);
            } catch (Exception e) {
                log.error("[SoundService] Failed to send danger alert. userId={}, label={}, prob={}",
                        userId, label, maxProb, e);
            }
        }

        long end = System.currentTimeMillis();
        long took = end - start;

        log.info("[SoundService] Detect done userId={}, label={}, prob={}, alert={}, took={}ms",
                userId, label, maxProb, alert, took);

        // 성공 로그 저장
        SoundDetectLog successLog = SoundDetectLog.successLog(
                user,
                filename,
                fileSize,
                label,
                maxProb,
                alert,
                probs.toString(), // 필요하면 JSON 포맷팅
                took
        );
        soundDetectLogRepository.save(successLog);

        return new DetectSoundResponse(label, maxProb, alert, probs);
    }

    /**
     * 어떤 소리를 위험으로 볼지 정책
     */
    private boolean isDangerousLabel(String label) {
        return "siren".equalsIgnoreCase(label)
                || "car_horn".equalsIgnoreCase(label);
    }

    /**
     * 내 소리 분석 기록 조회 (페이징)
     */
    public Page<SoundDetectLogRes> getMySoundLogs(Long userId, Pageable pageable) {
        Page<SoundDetectLog> page =
                soundDetectLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return page.map(SoundDetectLogRes::from);
    }
}
