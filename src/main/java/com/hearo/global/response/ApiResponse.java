package com.hearo.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int status;
    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    /* ---------- Factory: Success ---------- */
    public static <T> ResponseEntity<ApiResponse<T>> success(SuccessStatus s, T data) {
        return ResponseEntity.status(s.getStatus()).body(
                ApiResponse.<T>builder()
                        .status(s.getStatus().value())
                        .success(true)
                        .code(s.getCode())
                        .message(s.getMessage())
                        .data(data)
                        .build()
        );
    }

    public static ResponseEntity<ApiResponse<Void>> success(SuccessStatus s) {
        return ResponseEntity.status(s.getStatus()).body(
                ApiResponse.<Void>builder()
                        .status(s.getStatus().value())
                        .success(true)
                        .code(s.getCode())
                        .message(s.getMessage())
                        .build()
        );
    }

    /* ---------- Factory: Error ---------- */
    public static <T> ResponseEntity<ApiResponse<T>> error(ErrorStatus e) {
        return ResponseEntity.status(e.getStatus()).body(
                ApiResponse.<T>builder()
                        .status(e.getStatus().value())
                        .success(false)
                        .code(e.getCode())
                        .message(e.getMessage())
                        .build()
        );
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(ErrorStatus.ErrorWithDetail ew) {
        String combined = combine(ew.getMessage(), ew.getDetail());
        return ResponseEntity.status(ew.getStatus()).body(
                ApiResponse.<T>builder()
                        .status(ew.getStatus().value())
                        .success(false)
                        .code(ew.getCode())
                        .message(combined)
                        .build()
        );
    }

    private static String combine(String base, String detail) {
        if (detail == null || detail.isBlank()) return base;
        return base + " - " + detail;
    }
}
