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

        // 개발/디버깅용: JSON 응답
        if (shouldReturnJson(req)) {
            writeJson(res, access, refresh);
        } else {
            // 모바일 딥링크 리다이렉트
            // hearo://login?token=<ACCESS_JWT>
            String redirect = UriComponentsBuilder.fromUriString(authProps.getSuccessRedirect())
                    .queryParam("token", access)          // 프론트 요청: token=JWT토큰값
                    .build()
                    .encode()
                    .toUriString();

            getRedirectStrategy().sendRedirect(req, res, redirect);
        }
    }

    /**
     * JSON 응답으로 강제 전환하는 조건:
     * 1) URL 쿼리파라미터: ?debugJson=true
     * 2) 환경변수: APP_OAUTH2_SUCCESS_RESPONSE=json
     * 3) JVM 옵션: -Dapp.oauth2.success-response=json
     */
    private boolean shouldReturnJson(HttpServletRequest req) {
        // 1) 쿼리 파라미터
        String q = req.getParameter("debugJson");
        if ("true".equalsIgnoreCase(q)) return true;

        // 2) 환경변수
        String envVal = System.getenv("APP_OAUTH2_SUCCESS_RESPONSE");
        if ("json".equalsIgnoreCase(envVal)) return true;

        // 3) JVM 시스템 프로퍼티
        String sysVal = System.getProperty("app.oauth2.success-response");
        return "json".equalsIgnoreCase(sysVal);
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
