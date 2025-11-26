package com.hearo.sound.dto;

import java.util.Map;

public record AiInferResponse(
        String filename,
        String predicted,
        Map<String, Double> probs
) {}
