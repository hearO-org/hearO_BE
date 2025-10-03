package com.hearo.auth.controller;

import com.hearo.auth.dto.*;
import com.hearo.auth.service.AuthService;
import com.hearo.global.response.ApiResponse;
import com.hearo.global.response.SuccessStatus;
import com.hearo.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Map<String,Object>>> signup(@Valid @RequestBody SignupReq req) {
        var u = userService.createLocalUser(req.email(), req.password(), req.nickname());
        return ApiResponse.success(SuccessStatus.SIGNUP_SUCCESS,
                Map.of("id", u.getId(), "email", u.getEmail(), "nickname", u.getNickname()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenRes>> login(@Valid @RequestBody LoginReq req) {
        var token = authService.loginLocal(req);
        return ApiResponse.success(SuccessStatus.LOGIN_SUCCESS, token);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRes>> refresh(@RequestBody Map<String,String> body) {
        var token = authService.refresh(body.get("refresh"));
        return ApiResponse.success(SuccessStatus.OK, token);
    }

    @PostMapping("/logout") // 글로벌 로그아웃(전 기기)
    public ResponseEntity<ApiResponse<Void>> logoutAll(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        authService.logoutAll(userId);
        return ApiResponse.success(SuccessStatus.LOGOUT_SUCCESS);
    }

    @PostMapping("/logout/session")   // 현재 기기만 로그아웃
    public ResponseEntity<ApiResponse<Void>> logoutSession(@RequestBody Map<String,String> body) {
        authService.logoutSession(body.get("refresh"));
        return ApiResponse.success(SuccessStatus.LOGOUT_SUCCESS);
    }
}
