package com.hearo.member.controller;

import com.hearo.global.response.ApiResponse;
import com.hearo.global.response.SuccessStatus;
import com.hearo.member.dto.request.OnboardingReq;
import com.hearo.member.dto.request.UpdateMemberProfileReq;
import com.hearo.member.dto.response.MemberProfileRes;
import com.hearo.member.service.MemberProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberProfileController {

    private final MemberProfileService memberProfileService;

    /**
     * [POST] /api/v1/members/onboarding
     * - 최초 1회 온보딩 정보를 입력하는 엔드포인트
     * - JWT 인증 필수
     */
    @PostMapping("/onboarding")
    public ResponseEntity<ApiResponse<MemberProfileRes>> completeOnboarding(
            Authentication auth,
            @Valid @RequestBody OnboardingReq req
    ) {
        Long userId = (Long) auth.getPrincipal(); // JwtAuthFilter에서 userId를 principal로 넣고 있음
        MemberProfileRes res = memberProfileService.completeOnboarding(userId, req);
        return ApiResponse.success(SuccessStatus.CREATED, res);
    }

    /**
     * [GET] /api/v1/members/me
     * - 내 프로필 조회
     * - 온보딩 여부 확인용으로도 사용 가능
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileRes>> getMyProfile(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        MemberProfileRes res = memberProfileService.getMyProfile(userId);
        return ApiResponse.success(SuccessStatus.FETCHED, res);
    }

    /**
     * [PUT] /api/v1/members/me
     * - 내 프로필 수정
     * - JWT 인증 필수
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileRes>> updateMyProfile(
            Authentication auth,
            @Valid @RequestBody UpdateMemberProfileReq req
    ) {
        Long userId = (Long) auth.getPrincipal();
        MemberProfileRes res = memberProfileService.updateMyProfile(userId, req);

        return ApiResponse.success(SuccessStatus.UPDATED, res);
    }
}
