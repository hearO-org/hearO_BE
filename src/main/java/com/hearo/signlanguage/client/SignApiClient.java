package com.hearo.signlanguage.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hearo.signlanguage.client.dto.SignRawResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SignApiClient {

    private final WebClient signApiWebClient;

    @Value("${sign.api.service-key}")
    private String serviceKey;

    public SignRawResponse fetch(String keyword, int pageNo, int numOfRows) {
        String kw = (keyword == null) ? "" : keyword;

        String xml = signApiWebClient.get()
                .uri(uri -> uri.path("/API_CNV_054/request")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("pageNo", pageNo)
                        .queryParam("keyword", kw)
                        .queryParam("collectionDb", "")
                        .build())
                .accept(MediaType.APPLICATION_XML)  // XML로 받기
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (xml == null || xml.isBlank()) {
            throw new IllegalStateException("KCISA API 빈 응답");
        }

        try {
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            JsonNode xmlTree = xmlMapper.readTree(xml.getBytes(StandardCharsets.UTF_8));

            ObjectMapper jsonMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            var wrapped = jsonMapper.createObjectNode();
            wrapped.set("response", xmlTree);

            return jsonMapper.treeToValue(wrapped, SignRawResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("KCISA XML 파싱 실패", e);
        }
    }
}
