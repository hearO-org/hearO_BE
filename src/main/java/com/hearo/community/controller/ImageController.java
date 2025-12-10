package com.hearo.community.controller;

import com.hearo.community.dto.ImageUploadRes;
import com.hearo.global.response.ApiResponse;
import com.hearo.global.response.SuccessStatus;
import com.hearo.s3.service.S3ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/community/images")
@RequiredArgsConstructor
public class ImageController {

    private final S3ImageStorageService s3ImageStorageService;

    /**
     * 게시판 이미지 업로드
     * - form-data: files=[파일1, 파일2, ...]
     * - 최대 5장 제한
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadRes>> upload(
            Authentication authentication,
            @RequestPart("files") MultipartFile[] files
    ) {
        Long uid = requireUserId(authentication);

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        if (files.length > 5) {
            throw new IllegalArgumentException("이미지는 한 번에 최대 5장까지 업로드할 수 있습니다.");
        }

        List<String> urls = Arrays.stream(files)
                .map(f -> s3ImageStorageService.uploadOne(uid, f))
                .toList();

        return ApiResponse.success(SuccessStatus.CREATED, new ImageUploadRes(urls));
    }

    private Long requireUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null || !(auth.getPrincipal() instanceof Long uid) || uid <= 0) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }
        return uid;
    }
}
