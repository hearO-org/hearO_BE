package com.hearo.global;

import com.hearo.auth.handler.CustomOAuth2UserService;
import com.hearo.auth.handler.OAuth2FailureHandler;
import com.hearo.auth.handler.OAuth2SuccessHandler;
import com.hearo.global.jwt.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler successHandler;
    private final OAuth2FailureHandler failureHandler;

    /** 1) API 전용 체인: /api/** 에서는 OAuth2 리다이렉트 금지 + 미인증은 401 JSON */
    @Bean
    @Order(1)
    SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(c -> c.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .oauth2Login(o -> o.disable()) // ★ 핵심: API 영역에서는 OAuth2 비활성화(리다이렉트 절대 금지)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"error\":\"unauthorized\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/actuator/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 2) 웹 전용 체인: 나머지 경로에서만 카카오 OAuth2 허용 */
    @Bean
    @Order(2)
    SecurityFilterChain webChain(HttpSecurity http) throws Exception {
        http
                .csrf(c -> c.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login/**", "/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(o -> o
                        .userInfoEndpoint(u -> u.userService(oAuth2UserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                );
        return http.build();
    }
}
