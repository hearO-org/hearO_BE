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
    @PostMapping(
            value = "/detect",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE, //이 API는 multipart/form-data 로만 받음 그 외 다른 타입은 415 에러 반환
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<DetectSoundResponse>> detect(
            Authentication auth,
            @RequestPart("file") MultipartFile file
    ) throws Exception {

        System.out.println("=== [SoundController] /api/v1/sound/detect 진입 ===");

        if (file == null || file.isEmpty()) {
            throw new ApiException(
                    ErrorStatus.INVALID_INPUT,
                    "업로드된 오디오 파일이 비어 있습니다."
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new ApiException(
                    ErrorStatus.INVALID_INPUT,
                    "audio/* 타입의 파일만 허용됩니다. contentType=" + contentType
            );
        }

        Long userId = null;
        if (auth != null && auth.getPrincipal() instanceof Long id) {
            userId = id;
        }

        byte[] bytes = file.getBytes();
        DetectSoundResponse result = soundService.detect(bytes, file.getOriginalFilename(), userId);
        return ApiResponse.success(SuccessStatus.OK, result);
    }
}
