package com.hearo.job.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hearo.job.client.dto.JobRawResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class JobApiClient {

    private final WebClient jobApiWebClient;

    @Value("${job.api.path}")
    private String apiPath;

    @Value("${job.api.service-key}")
    private String serviceKey;

    private static final XmlMapper XML = (XmlMapper) new XmlMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public JobRawResponse fetch(int pageNo, int numOfRows) {
        String xml = jobApiWebClient.get()
                .uri(uri -> uri.path(apiPath)
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .build())
                .accept(MediaType.APPLICATION_XML)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (xml == null || xml.isBlank()) throw new IllegalStateException("Job API 빈 응답");
        try {
            return XML.readValue(xml, JobRawResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Job API XML 파싱 실패", e);
        }
    }
}
