package com.hearo.job.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hearo.job.client.dto.JobRawResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobApiClient {

    private final WebClient jobWebClient;

    @Value("${job.api.path}")         // "/B552583/job/job_list_env"
    private String jobPath;

    @Value("${job.api.service-key}")  // ★ data.go.kr '인코딩(Encoding)' 키를 그대로 주입 (%2F, %3D 등 포함)
    private String encodedServiceKey;

    public JobRawResponse fetch(int pageNo, int numOfRows) {
        // 1) XML을 문자열로 먼저 받기 (코덱 문제 회피)
        String xml = jobWebClient.get()
                .uri(b -> b.path(jobPath)
                        .queryParam("serviceKey", encodedServiceKey)             // 재인코딩 금지 (Config에서 NONE)
                        .queryParam("pageNo", Integer.toString(pageNo))
                        .queryParam("numOfRows", Integer.toString(numOfRows))
                        .build())
                .accept(MediaType.APPLICATION_XML)                               // application/xml;charset=utf-8 대응
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (xml == null || xml.isBlank()) {
            throw new IllegalStateException("외부 API 응답이 비어 있습니다.");
        }

        // 2) XmlMapper로 안전 파싱 (알 수 없는 필드 무시)
        try {
            XmlMapper xm = XmlMapper.builder()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .build();
            return xm.readValue(xml, JobRawResponse.class);
        } catch (Exception e) {
            log.warn("[JobApiClient] XML 파싱 실패: {}", e.toString());
            throw new IllegalStateException("외부 API XML 파싱 실패", e);
        }
    }
}
