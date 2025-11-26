package com.hearo.alert.controller;

import com.hearo.alert.domain.DeviceToken;
import com.hearo.alert.dto.DeactivateTokenReq;
import com.hearo.alert.dto.RegisterTokenReq;
import com.hearo.alert.repository.DeviceTokenRepository;
import com.hearo.global.response.ApiResponse;
import com.hearo.global.response.SuccessStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/alerts")
public class DeviceTokenController {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * 앱이 FCM device token을 서버에 등록하는 API
     */
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<Void>> registerToken(
            Authentication auth,
            @RequestBody RegisterTokenReq req
    ) {
        Long userId = (Long) auth.getPrincipal();

        // 이미 존재하는 토큰이면 업데이트, 없으면 새로 생성
        DeviceToken token = deviceTokenRepository.findByToken(req.token())
                .map(existing -> {
                    existing = DeviceToken.builder()
                            .id(existing.getId())
                            .userId(userId)
                            .token(existing.getToken())
                            .platform(req.platform())
                            .active(true)
                            .build();
                    return existing;
                })
                .orElseGet(() ->
                        DeviceToken.builder()
                                .userId(userId)
                                .token(req.token())
                                .platform(req.platform())
                                .active(true)
                                .build()
                );

        deviceTokenRepository.save(token);
        return ApiResponse.success(SuccessStatus.CREATED);
    }

    /**
     * 앱에서 로그아웃 / 알림 끄기 등으로 토큰 비활성화하고 싶을 때
     */
    @PostMapping("/token/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateToken(
            Authentication auth,
            @RequestBody DeactivateTokenReq req
    ) {
        Long userId = (Long) auth.getPrincipal();

        deviceTokenRepository.findByToken(req.token())
                .filter(t -> t.getUserId().equals(userId))
                .ifPresent(t -> {
                    DeviceToken updated = DeviceToken.builder()
                            .id(t.getId())
                            .userId(t.getUserId())
                            .token(t.getToken())
                            .platform(t.getPlatform())
                            .active(false)
                            .build();
                    deviceTokenRepository.save(updated);
                });

        return ApiResponse.success(SuccessStatus.UPDATED);
    }
}
