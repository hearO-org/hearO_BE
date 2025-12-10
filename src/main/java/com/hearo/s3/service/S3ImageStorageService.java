package com.hearo.s3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3ImageStorageService {

    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.base-url}")
    private String baseUrl;

    @Value("${app.s3.folder}")
    private String folder;

    // 허용 MIME 타입
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    // 최대 5MB
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024;

    public String uploadOne(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("비어있는 파일입니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("이미지 크기는 최대 5MB까지 가능합니다.");
        }

        String ext = getExtension(file.getOriginalFilename());
        String key = buildKey(userId, ext);

        try {
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(putReq, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
        }

        // 최종 접근 URL
        return baseUrl + "/" + key;
    }

    private String buildKey(Long userId, String ext) {
        String uuid = UUID.randomUUID().toString();
        // 예시) community/images/123/랜덤uuid.jpg
        return String.format("%s/%d/%s.%s", folder, userId, uuid, ext);
    }

    private String getExtension(String filename) {
        if (filename == null) return "jpg";
        int idx = filename.lastIndexOf('.');
        if (idx == -1 || idx == filename.length() - 1) {
            return "jpg";
        }
        return filename.substring(idx + 1).toLowerCase();
    }
}
