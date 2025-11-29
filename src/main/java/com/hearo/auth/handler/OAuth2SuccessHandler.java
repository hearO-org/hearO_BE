package com.hearo.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hearo.auth.domain.RefreshToken;
import com.hearo.auth.repository.RefreshTokenRepository;
import com.hearo.global.jwt.JwtProvider;
import com.hearo.global.props.HearoAuthProps;
import com.hearo.user.domain.User;
import com.hearo.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final JwtProvider jwt;
    private final HearoAuthProps authProps;
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth)
            throws IOException {

        OAuth2User o = (OAuth2User) auth.getPrincipal();
        String kakaoId = String.valueOf(o.getAttributes().get("id"));
        User u = users.findByKakaoId(kakaoId).orElseThrow();

        // refresh 저장 (회전/재사용 탐지용)
        String jti = UUID.randomUUID().toString();
        refreshTokens.save(RefreshToken.builder()
                .userId(u.getId())
                .jti(jti)
                .tokenVersion(u.getTokenVersion())
                .expiresAt(Instant.now().plusSeconds(jwt.refreshTtlSeconds()))
                .revoked(false)
                .build());

        String access  = jwt.createAccess(u.getId(), u.getTokenVersion(), List.of("ROLE_USER"));
        String refresh = jwt.createRefresh(u.getId(), u.getTokenVersion(), jti);

        if (shouldReturnJson(req)) {
            writeJson(res, access, refresh);
        } else {
            // hearo://login?token=<ACCESS>&refresh=<REFRESH>&tokenType=Bearer&expiresIn=...
            String redirect = UriComponentsBuilder.fromUriString(authProps.getSuccessRedirect())
                    .queryParam("token", access)         // token = access JWT
                    .queryParam("refresh", refresh)      // 세션 연장을 위한 refresh JWT
                    .queryParam("tokenType", "Bearer")
                    .queryParam("expiresIn", jwt.accessTtlSeconds())
                    .build()
                    .encode()
                    .toUriString();

            getRedirectStrategy().sendRedirect(req, res, redirect);
        }
    }

    // 디버깅용: ?debugJson=true 붙이면 JSON으로 토큰 확인 가능
    private boolean shouldReturnJson(HttpServletRequest req) {
        String q = req.getParameter("debugJson");
        return "true".equalsIgnoreCase(q);
    }

    private void writeJson(HttpServletResponse res, String access, String refresh) throws IOException {
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = Map.of(
                "status", 200,
                "success", true,
                "code", "LOGIN_SUCCESS",
                "message", "소셜 로그인에 성공했습니다.",
                "data", Map.of(
                        "access", access,
                        "refresh", refresh,
                        "tokenType", "Bearer",
                        "expiresIn", jwt.accessTtlSeconds()
                )
        );
        om.writeValue(res.getWriter(), body);
    }
}
