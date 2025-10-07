package com.hearo.signlanguage.controller;

import com.hearo.global.response.ApiResponse;
import com.hearo.global.response.SuccessStatus;
import com.hearo.signlanguage.domain.SignEntry;
import com.hearo.signlanguage.dto.IngestResultDto;
import com.hearo.signlanguage.dto.SignPageDto;
import com.hearo.signlanguage.service.SignService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/signs")
@RequiredArgsConstructor
public class SignController {

    private final SignService service;

    // 외부 API 그대로 조회
    @GetMapping("/external")
    public ResponseEntity<ApiResponse<SignPageDto>> externalList(
            @RequestParam(name = "pageNo", required = false) Integer pageNo,
            @RequestParam(name = "numOfRows", required = false) Integer numOfRows,
            @RequestParam(name = "page", required = false) Integer legacyPage,
            @RequestParam(name = "size", required = false) Integer legacySize
    ) {
        int p = (pageNo != null) ? pageNo : (legacyPage != null ? legacyPage : 1);
        int s = (numOfRows != null) ? numOfRows : (legacySize != null ? legacySize : 10);

        var data = service.externalList(p, s);
        return ApiResponse.success(SuccessStatus.FETCHED, data);
    }

    // 외부 API 키워드 검색
    @GetMapping("/external/search")
    public ResponseEntity<ApiResponse<SignPageDto>> externalSearch(
            @RequestParam String keyword,
            @RequestParam(name = "pageNo", required = false) Integer pageNo,
            @RequestParam(name = "numOfRows", required = false) Integer numOfRows,
            @RequestParam(name = "page", required = false) Integer legacyPage,
            @RequestParam(name = "size", required = false) Integer legacySize
    ) {
        int p = (pageNo != null) ? pageNo : (legacyPage != null ? legacyPage : 1);
        int s = (numOfRows != null) ? numOfRows : (legacySize != null ? legacySize : 10);

        var data = service.externalSearch(keyword, p, s);
        return ApiResponse.success(SuccessStatus.FETCHED, data);
    }

    // DB 조회
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SignEntry>>> listFromDb(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var data = service.listFromDb(page, size);
        return ApiResponse.success(SuccessStatus.FETCHED, data);
    }

    // DB 검색
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<SignEntry>>> searchFromDb(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var data = service.searchFromDb(keyword, page, size);
        return ApiResponse.success(SuccessStatus.FETCHED, data);
    }

    // 모든 수어 DB upsert
    @PostMapping("/ingest/all")
    public ResponseEntity<ApiResponse<IngestResultDto>> ingestAll(
            @RequestParam(defaultValue = "500") int size) {
        var res = service.ingestAll(size);
        return ApiResponse.success(SuccessStatus.OK, res);
    }
}
