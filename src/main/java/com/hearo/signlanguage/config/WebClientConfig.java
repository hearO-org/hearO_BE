package com.hearo.signlanguage.config;

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
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${spring.webclient.connect-timeout-ms}")
    private int connectTimeoutMs;

    @Value("${spring.webclient.read-timeout-ms}")
    private int readTimeoutMs;

    @Bean
    public WebClient signApiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs + 1000))
                .doOnConnected(c -> c.addHandler(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl("https://api.kcisa.kr")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter((req, next) -> {
                    long start = System.currentTimeMillis();
                    log.info("[REQ] {} {}", req.method(), req.url());
                    return next.exchange(req)
                            .doOnNext(res -> {
                                long took = System.currentTimeMillis() - start;
                                log.info("[RES] status={} url={} took={}ms", res.statusCode(), req.url(), took);
                            });
                })
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                        .build())
                .build();
    }
}
