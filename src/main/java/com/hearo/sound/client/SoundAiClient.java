package com.hearo.sound.client;

import com.hearo.sound.config.SoundAiConfig;
import com.hearo.sound.dto.AiInferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SoundAiClient {

    private final WebClient soundAiWebClient;
    private final SoundAiConfig config;

    public AiInferResponse infer(byte[] wavBytes, String originalFilename) {
        ByteArrayResource fileResource = new ByteArrayResource(wavBytes) {
            @Override
            public String getFilename() {
                return originalFilename != null ? originalFilename : "audio.wav";
            }
        };

        return soundAiWebClient.post()
                .uri(config.getInferPath())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", fileResource))
                .retrieve()
                .bodyToMono(AiInferResponse.class)
                .timeout(Duration.ofMillis(config.getTimeoutMs()))
                .onErrorResume(ex -> {
                    // TODO: 로거로 교체
                    ex.printStackTrace();
                    return Mono.empty();
                })
                .block();
    }
}
