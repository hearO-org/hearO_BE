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
                .securityMatcher("/api/**")   // 오직 /api/** 만
                .csrf(c -> c.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .oauth2Login(o -> o.disable())   // 여기서는 카카오 로그인 절대 X
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

    /** 2) 로그인 관련 엔드포인트만 카카오 OAuth2 사용 */
    @Bean
    @Order(1)
    SecurityFilterChain webChain(HttpSecurity http) throws Exception {
        http
                // 딱 이 3가지만: /, /login/**, /oauth2/**
                .securityMatcher("/", "/login/**", "/oauth2/**")
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
