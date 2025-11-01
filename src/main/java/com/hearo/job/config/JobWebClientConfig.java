package com.hearo.job.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * data.go.kr 장애인 구인정보 WebClient 설정 (XML 수신)
 */
@Configuration
@Slf4j
public class JobWebClientConfig {

    @Value("${spring.webclient.connect-timeout-ms}")
    private int connectTimeoutMs;

    @Value("${spring.webclient.read-timeout-ms}")
    private int readTimeoutMs;

    @Value("${job.api.base-url}")
    private String baseUrl;

    @Bean
    public WebClient jobApiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs + 1000))
                .doOnConnected(c -> c.addHandler(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE) // XML 수신
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter((req, next) -> {
                    long t0 = System.currentTimeMillis();
                    log.info("[JOB-REQ] {} {}", req.method(), req.url());
                    return next.exchange(req).doOnNext(res ->
                            log.info("[JOB-RES] status={} took={}ms url={}",
                                    res.statusCode(), System.currentTimeMillis() - t0, req.url()));
                })
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                        .build())
                .build();
    }
}
