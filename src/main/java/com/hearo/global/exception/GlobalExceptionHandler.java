package com.hearo.global.exception;

import com.hearo.global.dto.ErrorResponse;
import com.hearo.global.response.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직에서 명시적으로 던진 예외 처리
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        var es = e.getErrorStatus();
        log.warn("[API-ERROR] code={}, msg={}", es.getCode(), e.getMessage());
        return ResponseEntity
                .status(es.getStatus())
                .body(ErrorResponse.of(es.getCode(), e.getMessage()));
    }

    /**
     * @Valid 검증 실패 처리
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidation(Exception ex) {
        String message = "유효하지 않은 입력입니다.";

        if (ex instanceof MethodArgumentNotValidException manv) {
            var errors = manv.getBindingResult().getFieldErrors();
            if (!errors.isEmpty()) {
                var fe = errors.get(0);
                message = fe.getField() + ": " + fe.getDefaultMessage();
            }
        } else if (ex instanceof BindException be) {
            var errors = be.getBindingResult().getFieldErrors();
            if (!errors.isEmpty()) {
                var fe = errors.get(0);
                message = fe.getField() + ": " + fe.getDefaultMessage();
            }
        }

        log.warn("[VALIDATION] {}", message);
        var es = ErrorStatus.INVALID_INPUT;
        return ResponseEntity
                .status(es.getStatus())
                .body(ErrorResponse.of(es.getCode(), message));
    }

    /**
     * 잘못된 인자 전달 시 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[ILLEGAL-ARG] {}", e.getMessage());
        var es = ErrorStatus.INVALID_INPUT;
        return ResponseEntity
                .status(es.getStatus())
                .body(ErrorResponse.of(es.getCode(), e.getMessage()));
    }

    /**
     * 처리되지 않은 예외(서버 내부 오류)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("[UNEXPECTED] {}", e.getMessage(), e);
        var es = ErrorStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(es.getStatus())
                .body(ErrorResponse.of(es.getCode(), es.getMessage()));
    }
}
