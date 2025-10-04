package com.hearo.global.jwt;

public record JwtPayload(Long userId, String typ, Integer ver, String jti) {}
