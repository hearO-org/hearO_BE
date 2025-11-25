package com.hearo.sound.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sound.ai")
public class SoundAiConfig {

    private String baseUrl;

    private String inferPath = "/infer";

    /**
     * WebClient timeout (ms)
     */
    private long timeoutMs = 3000;

    /**
     * 위험으로 판단할 최소 확률 (예: 0.7 이상이면 alert = true)
     */
    private double minAlertProb = 0.7;

    @Bean
    public WebClient soundAiWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
