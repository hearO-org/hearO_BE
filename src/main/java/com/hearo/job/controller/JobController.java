package com.hearo.job.controller;

import com.hearo.global.response.ApiResponse;
import com.hearo.global.response.ErrorStatus;
import com.hearo.global.response.SuccessStatus;
import com.hearo.job.dto.JobDetailDto;
import com.hearo.job.dto.JobFilter;
import com.hearo.job.dto.JobPageDto;
import com.hearo.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 구인정보 API
 * - 목록 프록시 / 서버사이드 검색 / 상세 / 팩셋
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService service;

    /** 1) 원본 목록 프록시 */
    @GetMapping("/external")
    public ResponseEntity<ApiResponse<JobPageDto>> externalList(
            @RequestParam(name = "pageNo", required = false) Integer pageNo,
            @RequestParam(name = "numOfRows", required = false) Integer numOfRows,
            @RequestParam(name = "page", required = false) Integer legacyPage,
            @RequestParam(name = "size", required = false) Integer legacySize
    ) {
        int p = (pageNo != null) ? pageNo : (legacyPage != null ? legacyPage : 1);
        int s = (numOfRows != null) ? numOfRows : (legacySize != null ? legacySize : 100);
        if (p < 1) p = 1;
        if (s < 1) s = 10;

        try {
            var data = service.externalList(p, s);
            return ApiResponse.success(SuccessStatus.FETCHED, data);

        } catch (WebClientResponseException e) {
            // 외부 응답 오류
            if (e.getStatusCode().value() == 429) {
                return ApiResponse.error(ErrorStatus.EXTERNAL_QUOTA);
            }
            return ApiResponse.error(ErrorStatus.EXTERNAL_ERROR.withDetail(e.getMessage()));

        } catch (WebClientRequestException e) {
            // 네트워크/타임아웃 등 요청 예외
            return ApiResponse.error(ErrorStatus.EXTERNAL_ERROR.withDetail("네트워크 오류"));

        } catch (IllegalStateException e) {
            // 파싱 실패/빈 응답 등 내부 처리 중 오류
            return ApiResponse.error(ErrorStatus.EXTERNAL_ERROR.withDetail(e.getMessage()));
        }
    }

    /** 2) 서버사이드 필터 검색 (region 포함) */
    @GetMapping("/external/search")
    public ResponseEntity<ApiResponse<JobPageDto>> externalSearch(
            JobFilter filter,                         // QueryParam → 바인딩
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;

        try {
            var data = service.externalFiltered(filter, page, size);
            return ApiResponse.success(SuccessStatus.FETCHED, data);

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                return ApiResponse.error(ErrorStatus.EXTERNAL_QUOTA);
            }
            return ApiResponse.error(ErrorStatus.EXTERNAL_ERROR.withDetail(e.getMessage()));

        } catch (WebClientRequestException e) {
            return ApiResponse.error(ErrorStatus.EXTERNAL_ERROR.withDetail("네트워크 오류"));

        } catch (IllegalStateException | IllegalArgumentException e) {
            return ApiResponse.error(ErrorStatus.EXTERNAL_ERROR.withDetail(e.getMessage()));
        }
    }

    /** 3) 상세 (rno 기준, 캐시 사용) */
    @GetMapping("/external/{rno}")
    public ResponseEntity<ApiResponse<JobDetailDto>> getDetail(@PathVariable String rno) {
        try {
            var dto = service.getDetailByRno(rno);
            return ApiResponse.success(SuccessStatus.FETCHED, dto);
        } catch (IllegalArgumentException e) {
            // rno 미발견
            return ApiResponse.error(ErrorStatus.NOT_FOUND.withDetail(e.getMessage()));
        }
    }

    /** 4) (옵션) 필터 facet 값 (드롭다운용 샘플) */
    @GetMapping("/external/facets")
    public ResponseEntity<ApiResponse<Map<String, Object>>> facets(
            @RequestParam(defaultValue = "100") int numOfRows
    ) {
        if (numOfRows < 1) numOfRows = 50;
        var page = service.externalList(1, numOfRows);

        Set<String> empTypes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> salaryTypes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> enterTypes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        page.getItems().forEach(i -> {
            if (i.getEmpType() != null && !i.getEmpType().isBlank()) empTypes.add(i.getEmpType());
            if (i.getSalaryType() != null && !i.getSalaryType().isBlank()) salaryTypes.add(i.getSalaryType());
            if (i.getEnterType() != null && !i.getEnterType().isBlank()) enterTypes.add(i.getEnterType());
        });

        return ApiResponse.success(SuccessStatus.FETCHED, Map.of(
                "empTypes", empTypes,
                "salaryTypes", salaryTypes,
                "enterTypes", enterTypes
        ));
    }
}
