package com.hearo.sound.dto;

import java.util.Map;

public record DetectSoundResponse(
        String label,         // 예: "siren"
        double confidence,    // max prob
        boolean alert,        // 알림을 울릴 수준인지
        Map<String, Double> probs
) {}
