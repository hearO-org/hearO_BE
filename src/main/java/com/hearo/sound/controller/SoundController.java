package com.hearo.sound.controller;

import com.hearo.global.exception.ApiException;
import com.hearo.global.response.ApiResponse;
import com.hearo.global.response.ErrorStatus;
import com.hearo.global.response.SuccessStatus;
import com.hearo.sound.dto.DetectSoundResponse;
import com.hearo.sound.service.SoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sound")
public class SoundController {

    private final SoundService soundService;

    /**
     * 모바일 앱이 1~2초짜리 wav 조각을 올려서
     * 위험 소리인지 확인하는 엔드포인트.
     */
    @PostMapping(value = "/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DetectSoundResponse>> detect(
            Authentication auth,
            @RequestPart("file") MultipartFile file
    ) throws Exception {

        if (file.isEmpty()) {
            // 400 에러 공통 처리 타도록 예외 던지기
            throw new ApiException(
                    ErrorStatus.INVALID_INPUT,
                    "업로드된 오디오 파일이 비어 있습니다."
            );
        }

        // 간단한 MIME 체크
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new ApiException(
                    ErrorStatus.INVALID_INPUT,
                    "audio/* 타입의 파일만 허용됩니다. contentType=" + contentType
            );
        }

        Long userId = (Long) auth.getPrincipal(); // JwtAuthFilter에서 userId를 principal로 넣고 있음
        byte[] bytes = file.getBytes();

        DetectSoundResponse result = soundService.detect(bytes, file.getOriginalFilename(), userId);
        return ApiResponse.success(SuccessStatus.OK, result);
    }
}
