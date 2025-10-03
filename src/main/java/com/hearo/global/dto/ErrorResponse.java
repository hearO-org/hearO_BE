package com.hearo.global.dto;

public record ErrorResponse(
        String code,
        String message,
        Object data // 세부 원인/필드 오류 등을 담고 싶을 때 사용 (nullable)
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }
    public static ErrorResponse of(String code, String message, Object data) {
        return new ErrorResponse(code, message, data);
    }
}
