package com.hearo.auth.service;

import com.hearo.auth.domain.RefreshToken;
import com.hearo.auth.dto.LoginReq;
import com.hearo.auth.dto.TokenRes;
import com.hearo.auth.repository.RefreshTokenRepository;
import com.hearo.global.exception.ApiException;
import com.hearo.global.jwt.JwtPayload;
import com.hearo.global.jwt.JwtProvider;
import com.hearo.global.response.ErrorStatus;
import com.hearo.user.domain.User;
import com.hearo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtProvider jwt;

    /* ---------- 일반 로그인 ---------- */
    @Transactional
    public TokenRes loginLocal(LoginReq req) {
        User u = users.findByEmail(req.email())
                .filter(x -> x.getAuthType()== User.AuthType.LOCAL && Boolean.TRUE.equals(x.getEnabled()))
                .orElseThrow(() -> new ApiException(ErrorStatus.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (!encoder.matches(req.password(), u.getPasswordHash()))
            throw new ApiException(ErrorStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다.");

        return issueTokens(u);
    }

    /* ---------- 리프레시: 회전 + 재사용 탐지 ---------- */
    @Transactional
    public TokenRes refresh(String refreshToken) {
        JwtPayload p = jwt.verify(refreshToken);
        if (!"REFRESH".equals(p.typ()))
            throw new ApiException(ErrorStatus.REFRESH_TOKEN_INVALID);

        User u = users.findById(p.userId())
                .orElseThrow(() -> new ApiException(ErrorStatus.USER_NOT_FOUND));

        if (p.ver() == null || p.ver() != u.getTokenVersion())
            throw new ApiException(ErrorStatus.REFRESH_TOKEN_INVALID, "버전 불일치");

        if (p.jti() == null)
            throw new ApiException(ErrorStatus.REFRESH_TOKEN_INVALID, "JTI 누락");

        RefreshToken token = refreshTokens.findByJti(p.jti())
                .orElseThrow(() -> new ApiException(ErrorStatus.REFRESH_TOKEN_INVALID));

        // 재사용/만료/회수 감지
        if (token.isExpired() || token.isRevoked()) {
            u.bumpTokenVersion();              // 전 세션 무효화
            users.save(u);
            throw new ApiException(ErrorStatus.REFRESH_TOKEN_INVALID, "재사용 감지 - 전체 로그아웃");
        }

        // 회전: 기존 토큰 revoke + 새 jti 발급
        String newJti = UUID.randomUUID().toString();
        token.revoke(newJti);
        refreshTokens.save(token);

        RefreshToken newRt = RefreshToken.builder()
                .userId(u.getId())
                .jti(newJti)
                .tokenVersion(u.getTokenVersion())
                .expiresAt(Instant.now().plusSeconds(jwt.refreshTtlSeconds()))
                .revoked(false)
                .build();
        refreshTokens.save(newRt);

        String access  = jwt.createAccess(u.getId(), u.getTokenVersion(), List.of("ROLE_USER"));
        String refresh = jwt.createRefresh(u.getId(), u.getTokenVersion(), newJti);
        return new TokenRes(access, refresh, "Bearer", jwt.accessTtlSeconds());
    }

    /* ---------- 글로벌 로그아웃(전 세션 무효화) ---------- */
    @Transactional
    public void logoutAll(Long userId) {
        User u = users.findById(userId).orElseThrow(() -> new ApiException(ErrorStatus.USER_NOT_FOUND));
        u.bumpTokenVersion(); // 전 세션 만료
        users.save(u);
        // (선택) userId 기준 refresh 일괄 revoke 처리 가능
    }

    /* ---------- 현재 기기 로그아웃(리프레시 jti만 취소) ---------- */
    @Transactional
    public void logoutSession(String refreshToken) {
        JwtPayload p = jwt.verify(refreshToken);
        if (!"REFRESH".equals(p.typ()) || p.jti() == null)
            throw new ApiException(ErrorStatus.REFRESH_TOKEN_INVALID);

        RefreshToken t = refreshTokens.findByJti(p.jti())
                .orElseThrow(() -> new ApiException(ErrorStatus.REFRESH_TOKEN_INVALID));

        t.revokeOnly();
        refreshTokens.save(t);
    }

    /* ---------- 내부: 토큰 발급 + refresh 저장 ---------- */
    private TokenRes issueTokens(User u) {
        String jti = UUID.randomUUID().toString();

        RefreshToken rt = RefreshToken.builder()
                .userId(u.getId())
                .jti(jti)
                .tokenVersion(u.getTokenVersion())
                .expiresAt(Instant.now().plusSeconds(jwt.refreshTtlSeconds()))
                .revoked(false)
                .build();
        refreshTokens.save(rt);

        String access  = jwt.createAccess(u.getId(), u.getTokenVersion(), List.of("ROLE_USER"));
        String refresh = jwt.createRefresh(u.getId(), u.getTokenVersion(), jti);
        return new TokenRes(access, refresh, "Bearer", jwt.accessTtlSeconds());
    }
}
