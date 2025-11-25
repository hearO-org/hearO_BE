package com.hearo.sound.service;

import com.hearo.alert.service.NotificationService;
import com.hearo.sound.client.SoundAiClient;
import com.hearo.sound.config.SoundAiConfig;
import com.hearo.sound.dto.AiInferResponse;
import com.hearo.sound.dto.DetectSoundResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.Map;

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
        AiInferResponse aiRes = aiClient.infer(wavBytes, filename);
        if (aiRes == null || CollectionUtils.isEmpty(aiRes.probs())) {
            // AI 서버 에러일 때 fallback 전략
            return new DetectSoundResponse(
                    "unknown", 0.0, false, Map.of()
            );
        }

        // 최대 확률 라벨 찾아내기
        Map.Entry<String, Double> maxEntry = aiRes.probs().entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .orElseThrow();

        String label = maxEntry.getKey();
        double maxProb = maxEntry.getValue();

        boolean alert = maxProb >= config.getMinAlertProb()
                && isDangerousLabel(label);

        // 위험 소리라면 알림 발송 시도
        if (alert && userId != null) {
            notificationService.sendDangerAlert(userId, label, maxProb);
        }

        return new DetectSoundResponse(label, maxProb, alert, aiRes.probs());
    }

    // 어떤 소리에 알림을 줄지 정책 정의
    private boolean isDangerousLabel(String label) {
        // 예시: "siren", "car_horn"은 위험으로 간주
        return "siren".equalsIgnoreCase(label)
                || "car_horn".equalsIgnoreCase(label);
    }
}
