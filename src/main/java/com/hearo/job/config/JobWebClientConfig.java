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
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class JobWebClientConfig {

    @Value("${job.api.base-url}")
    private String baseUrl;

    @Value("${spring.webclient.connect-timeout-ms:4000}")
    private int connectTimeoutMs;

    @Value("${spring.webclient.read-timeout-ms:5000}")
    private int readTimeoutMs;

    @Bean
    public WebClient jobWebClient() {
        // 쿼리 재인코딩 금지 (serviceKey 그대로 전달)
        DefaultUriBuilderFactory ubf = new DefaultUriBuilderFactory(baseUrl);
        ubf.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs))
                .doOnConnected(c -> c.addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS)));

        // 기본 코덱만 사용 (jackson-dataformat-xml 존재시 Jackson2XmlDecoder 자동 활성화)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .uriBuilderFactory(ubf)
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .filter((req, next) -> {
                    long t0 = System.currentTimeMillis();
                    log.info("[JOB-REQ] {} {}", req.method(), req.url());
                    return next.exchange(req)
                            .doOnNext(res -> log.info(
                                    "[JOB-RES] status={} took={}ms url={}",
                                    res.statusCode(), (System.currentTimeMillis()-t0), req.url()));
                })
                .build();
    }
}
