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

    public DetectSoundResponse detect(byte[] wavBytes, String filename, Long userId) {

        long start = System.currentTimeMillis();
        long fileSize = wavBytes != null ? wavBytes.length : 0L;

        User user = userService.getById(userId);

        AiInferResponse aiRes;
        try {
            aiRes = aiClient.infer(wavBytes, filename);
        } catch (Exception e) {
            long took = System.currentTimeMillis() - start;

            log.error("[SoundService] AI infer failed. filename={}", filename, e);

            soundDetectLogRepository.save(
                    SoundDetectLog.failLog(
                            user, filename, fileSize,
                            "[AI 호출 실패] " + e.getMessage(),
                            took
                    )
            );
            return new DetectSoundResponse("unknown", 0.0, false, Map.of());
        }

        // 응답 자체가 없는 경우
        if (aiRes == null) {
            long took = System.currentTimeMillis() - start;

            soundDetectLogRepository.save(
                    SoundDetectLog.failLog(
                            user, filename, fileSize,
                            "[AI 응답 없음] null response",
                            took
                    )
            );
            return new DetectSoundResponse("unknown", 0.0, false, Map.of());
        }

        // detected=false 같은 케이스를 안전 처리
        if (Boolean.FALSE.equals(aiRes.detected())) {
            long took = System.currentTimeMillis() - start;

            SoundDetectLog logEntity = SoundDetectLog.successLog(
                    user,
                    filename,
                    fileSize,
                    "none",
                    0.0,
                    false,
                    (aiRes.probs() != null ? aiRes.probs().toString() : null),
                    took
            );
            soundDetectLogRepository.save(logEntity);

            return new DetectSoundResponse("none", 0.0, false, Map.of());
        }

        Map<String, Double> probs = aiRes.probs();
        if (probs == null || probs.isEmpty()) {
            long took = System.currentTimeMillis() - start;

            soundDetectLogRepository.save(
                    SoundDetectLog.failLog(
                            user, filename, fileSize,
                            "[AI 응답 없음] probs가 비어 있음",
                            took
                    )
            );
            return new DetectSoundResponse("unknown", 0.0, false, Map.of());
        }

        Map.Entry<String, Double> maxEntry = probs.entrySet().stream()
                .max(Comparator.comparingDouble(e -> e.getValue() != null ? e.getValue() : 0.0))
                .orElse(null);

        if (maxEntry == null || maxEntry.getKey() == null) {
            long took = System.currentTimeMillis() - start;

            soundDetectLogRepository.save(
                    SoundDetectLog.failLog(
                            user, filename, fileSize,
                            "[AI 응답 파싱 실패] maxEntry 없음",
                            took
                    )
            );
            return new DetectSoundResponse("unknown", 0.0, false, probs);
        }

        String label = maxEntry.getKey();
        double maxProb = maxEntry.getValue() != null ? maxEntry.getValue() : 0.0;

        // confidence 필드가 오면 참고(단, 최종은 probs 기반 maxProb 유지)
        if (aiRes.confidence() != null && aiRes.confidence() > 0.0) {
            log.debug("[SoundService] aiRes.confidence={} maxProb={}", aiRes.confidence(), maxProb);
        }

        boolean alert = maxProb >= config.getMinAlertProb()
                && isDangerousLabel(label);

        if (alert) {
            try {
                notificationService.sendDangerAlert(userId, label, maxProb);
            } catch (Exception e) {
                log.error("[SoundService] Failed to send danger alert. userId={}, label={}, prob={}",
                        userId, label, maxProb, e);
            }
        }

        long took = System.currentTimeMillis() - start;

        soundDetectLogRepository.save(
                SoundDetectLog.successLog(
                        user,
                        filename,
                        fileSize,
                        label,
                        maxProb,
                        alert,
                        probs.toString(),
                        took
                )
        );

        return new DetectSoundResponse(label, maxProb, alert, probs);
    }

    private boolean isDangerousLabel(String label) {
        return "siren".equalsIgnoreCase(label)
                || "car_horn".equalsIgnoreCase(label);
    }

    public Page<SoundDetectLogRes> getMySoundLogs(Long userId, Pageable pageable) {
        return soundDetectLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(SoundDetectLogRes::from);
    }
}
