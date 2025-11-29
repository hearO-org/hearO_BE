package com.hearo.sound.dto;

import com.hearo.sound.domain.SoundDetectLog;

import java.time.LocalDateTime;

public record SoundDetectLogRes(
        Long id,
        String label,           // 감지된 소리 (ex. siren, car_horn)
        boolean alert,          // 위험 여부
        boolean success,        // AI 호출 성공 여부
        double confidence,      // max 확률 (원시 값)
        String confidenceLevel, // HIGH / MEDIUM / LOW (유저용)
        LocalDateTime detectedAt // createdAt 그대로 노출
) {

    public static SoundDetectLogRes from(SoundDetectLog log) {
        double conf = log.getConfidence() != null ? log.getConfidence() : 0.0;

        String level;
        if (conf >= 0.8) {
            level = "HIGH";
        } else if (conf >= 0.5) {
            level = "MEDIUM";
        } else {
            level = "LOW";
        }

        return new SoundDetectLogRes(
                log.getId(),
                log.getLabel(),
                Boolean.TRUE.equals(log.getAlert()),
                Boolean.TRUE.equals(log.getSuccess()),
                conf,
                level,
                log.getCreatedAt()
        );
    }
}
