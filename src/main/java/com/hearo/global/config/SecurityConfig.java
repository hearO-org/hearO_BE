package com.hearo.global.config;

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

    /** 1) /api/** 전용 체인 (JWT, OAuth2 X) */
    @Bean
    @Order(0)
    SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http
                // 오직 /api/** 만 처리
                .securityMatcher("/api/**")
                .csrf(c -> c.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .oauth2Login(o -> o.disable())   // API에서는 OAuth2 로그인 절대 X
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"error\":\"unauthorized\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 열어줄 API만 여기에서 허용
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 2) 나머지 전체(/**)를 처리하는 웹/OAuth2 체인 (fallback 역할) */
    @Bean
    @Order(1)
    SecurityFilterChain webChain(HttpSecurity http) throws Exception {
        http
                // /api/** 를 제외한 나머지 모든 경로 처리
                .securityMatcher("/**")
                .csrf(c -> c.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 누구나 접근 가능
                        .requestMatchers("/", "/login/**", "/oauth2/**").permitAll()
                        // 그 외는 전부 인증 필요
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
