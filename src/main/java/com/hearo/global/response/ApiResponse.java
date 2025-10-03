package com.hearo.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** HTTP status code (e.g., 200, 201, 400 ...) */
    private final int status;

    /** true = success, false = error */
    private final boolean success;

    /** business code like OK, USER_NOT_FOUND ... */
    private final String code;

    /** human readable message */
    private final String message;

    /** payload (nullable on error) */
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

    /* ---------- Factory: Error (편의용) ---------- */
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
}
