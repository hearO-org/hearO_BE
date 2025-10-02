package com.hearo.global;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API 서버 기본값
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/actuator/health",
                        "/auth/**",
                        "/login/**",
                        "/oauth2/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(Customizer.withDefaults()) // /login/oauth2/code/kakao
            .logout(logout -> logout.logoutUrl("/auth/logout"));

        return http.build();
    }
}
