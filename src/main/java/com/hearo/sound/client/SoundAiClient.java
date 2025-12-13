package com.hearo.sound.client;

import com.hearo.sound.config.SoundAiConfig;
import com.hearo.sound.dto.AiInferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class SoundAiClient {

    private final WebClient soundAiWebClient;
    private final SoundAiConfig config;

    public AiInferResponse infer(byte[] wavBytes, String originalFilename) {

        if (wavBytes == null || wavBytes.length == 0) {
            log.warn("[SoundAI] infer called with empty bytes");
            return null;
        }

        String safeName = (originalFilename != null && !originalFilename.isBlank())
                ? originalFilename
                : "audio.wav";

        ByteArrayResource fileResource = new ByteArrayResource(wavBytes) {
            @Override
            public String getFilename() {
                return safeName;
            }
        };

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", fileResource)
                .filename(safeName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        return soundAiWebClient.post()
                .uri(config.getInferPath())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("[SoundAI] AI server error status={} body={}",
                                            resp.statusCode().value(), body);
                                    return Mono.error(new IllegalStateException(
                                            "AI server error: " + resp.statusCode().value()));
                                })
                )
                .bodyToMono(AiInferResponse.class)
                .timeout(Duration.ofMillis(config.getTimeoutMs()))
                .doOnError(ex -> log.error("[SoundAI] AI 서버 호출 실패: {}", ex.getMessage(), ex))
                .onErrorResume(ex -> Mono.empty())
                .block();
    }
}
