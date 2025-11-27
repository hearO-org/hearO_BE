package com.hearo.sound.service;

import com.hearo.alert.service.NotificationService;
import com.hearo.sound.client.SoundAiClient;
import com.hearo.sound.config.SoundAiConfig;
import com.hearo.sound.dto.AiInferResponse;
import com.hearo.sound.dto.DetectSoundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * @param wavBytes  1~2초짜리 WAV 바이너리
     * @param filename  원본 파일 이름
     * @param userId    현재 사용자 ID (알림 보낼 대상)
     */
    public DetectSoundResponse detect(byte[] wavBytes, String filename, Long userId) {

        long start = System.currentTimeMillis();

        // 1) AI 서버 호출 (예외 안전하게 처리)
        AiInferResponse aiRes;
        try {
            aiRes = aiClient.infer(wavBytes, filename);
        } catch (Exception e) {
            // AI 서버 연결 실패 / 타임아웃 등
            log.error("[SoundService] AI infer failed. filename={}", filename, e);
            // 서비스는 에러 대신 graceful fallback
            return new DetectSoundResponse("unknown", 0.0, false, Map.of());
        }

        // 2) 응답이 비었을 때 fallback
        if (aiRes == null || CollectionUtils.isEmpty(aiRes.probs())) {
            log.warn("[SoundService] AI response empty. filename={}", filename);
            return new DetectSoundResponse("unknown", 0.0, false, Map.of());
        }

        Map<String, Double> probs = aiRes.probs();

        // 3) 최대 확률 라벨 찾기
        Map.Entry<String, Double> maxEntry = probs.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .orElseThrow(() ->
                        new IllegalStateException("probs is not empty but maxEntry not found")
                );

        String label = maxEntry.getKey();
        double maxProb = maxEntry.getValue();

        // 4) 알림 여부 결정 (확률 + 위험 라벨 정책)
        boolean alert = maxProb >= config.getMinAlertProb()
                && isDangerousLabel(label);

        // 5) 위험 소리라면 알림 발송 시도 (실패해도 detect 자체는 성공)
        if (alert && userId != null) {
            try {
                notificationService.sendDangerAlert(userId, label, maxProb);
            } catch (Exception e) {
                log.error("[SoundService] Failed to send danger alert. userId={}, label={}, prob={}",
                        userId, label, maxProb, e);
                // 알림 실패만 로그 찍고, detect 결과는 그대로 리턴
            }
        }

        long end = System.currentTimeMillis();
        log.info("[SoundService] Detect done userId={}, label={}, prob={}, alert={}, took={}ms",
                userId, label, maxProb, alert, (end - start));

        return new DetectSoundResponse(label, maxProb, alert, probs);
    }

    /**
     * 어떤 소리를 위험으로 볼지 정책
     */
    private boolean isDangerousLabel(String label) {
        return "siren".equalsIgnoreCase(label)
                || "car_horn".equalsIgnoreCase(label);
    }
}
