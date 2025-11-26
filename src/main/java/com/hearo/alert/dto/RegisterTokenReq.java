package com.hearo.alert.dto;

public record RegisterTokenReq(
        String token,
        String platform   // "ANDROID", "IOS"
) {}
