package com.hearo.sound.controller;

import com.hearo.global.exception.ApiException;
import com.hearo.global.response.ApiResponse;
import com.hearo.global.response.ErrorStatus;
import com.hearo.global.response.SuccessStatus;
import com.hearo.sound.dto.DetectSoundResponse;
import com.hearo.sound.service.SoundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sound")
public class SoundController {

    // 일단 데이터 크기 제한은 3MB로
    private static final long MAX_FILE_SIZE = 3 * 1024 * 1024L;

    private final SoundService soundService;

    /**
     * 모바일 앱이 1~2초짜리 wav 조각을 올려서
     * 위험 소리인지 확인하는 엔드포인트.
     */
    @PostMapping(
            value = "/detect",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE, // 이 API는 multipart/form-data 로만 받음
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<DetectSoundResponse>> detect(
            Authentication auth,
            @RequestPart("file") MultipartFile file
    ) {

        Long userId = null;
        if (auth != null && auth.getPrincipal() instanceof Long id) {
            userId = id;
        }

        log.info("[SoundController] /api/v1/sound/detect called userId={}, size={}, contentType={}",
                userId,
                file != null ? file.getSize() : null,
                file != null ? file.getContentType() : null
        );

        // 1) 파일 유효성 체크
        if (file == null || file.isEmpty()) {
            throw new ApiException(
                    ErrorStatus.INVALID_INPUT,
                    "업로드된 오디오 파일이 비어 있습니다."
            );
        }

        // 2) 용량 제한 체크
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException(
                    ErrorStatus.INVALID_INPUT,
                    "파일 용량이 너무 큽니다. 최대 3MB까지 허용됩니다."
            );
        }

        // 3) content-type 체크 (audio/* 만 허용)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new ApiException(
                    ErrorStatus.INVALID_INPUT,
                    "audio/* 타입의 파일만 허용됩니다. contentType=" + contentType
            );
        }

        try {
            // 4) 실제 분석 호출
            byte[] bytes = file.getBytes();
            DetectSoundResponse result =
                    soundService.detect(bytes, file.getOriginalFilename(), userId);

            // 공통 성공 응답 포맷 사용
            return ApiResponse.success(SuccessStatus.OK, result);

        } catch (ApiException e) {
            // 이미 우리 쪽 비즈니스 예외라면 그대로 전파 → GlobalExceptionHandler에서 처리
            throw e;
        } catch (Exception e) {
            // 예상 못한 예외 → 내부 서버 에러로 래핑
            log.error("소리 분석 중 알 수 없는 오류 발생 userId={}", userId, e);
            throw new ApiException(
                    ErrorStatus.INTERNAL_SERVER_ERROR,
                    "소리 분석 중 오류가 발생했습니다."
            );
        }
    }
}
