package com.hearo.auth.dto;

public record TokenRes(
        String access,
        String refresh,
        String tokenType,
        long   expiresIn
) {}
