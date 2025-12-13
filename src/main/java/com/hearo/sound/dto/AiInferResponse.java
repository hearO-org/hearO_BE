package com.hearo.sound.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiInferResponse(
        String filename,
        Boolean detected,
        String predicted,
        Double confidence,
        Map<String, Double> probs
) {}
