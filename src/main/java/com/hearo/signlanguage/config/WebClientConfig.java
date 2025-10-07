package com.hearo.signlanguage.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${spring.webclient.connect-timeout-ms}")
    private int connectTimeoutMs;

    @Value("${spring.webclient.read-timeout-ms}")
    private int readTimeoutMs;

    @Bean
    public WebClient signApiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .doOnConnected(c -> c.addHandler(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl("https://api.kcisa.kr")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                        .build())
                .build();
    }
}
