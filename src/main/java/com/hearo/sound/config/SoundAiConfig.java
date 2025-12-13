package com.hearo.sound.config;

import io.netty.channel.ChannelOption;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sound.ai")
public class SoundAiConfig {

    private String baseUrl;
    private String inferPath = "/infer";

    /** 전체 응답 타임아웃 (ms) */
    private long timeoutMs = 3000;

    /** 위험으로 판단할 최소 확률 */
    private double minAlertProb = 0.7;

    @Bean
    public WebClient soundAiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeoutMs)
                .responseTimeout(Duration.ofMillis(timeoutMs));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
