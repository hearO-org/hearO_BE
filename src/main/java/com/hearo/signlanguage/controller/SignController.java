// file: src/main/java/com/hearo/signlanguage/controller/SignController.java
package com.hearo.signlanguage.controller;

import com.hearo.global.response.ApiResponse;
import com.hearo.global.response.ErrorStatus;
import com.hearo.global.response.SuccessStatus;
import com.hearo.signlanguage.domain.SignEntry;
import com.hearo.signlanguage.dto.IngestResultDto;
import com.hearo.signlanguage.dto.SignDetailDto;
import com.hearo.signlanguage.dto.SignFavoriteItemDto;
import com.hearo.signlanguage.dto.SignPageDto;
import com.hearo.signlanguage.service.SignService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/signs")
@RequiredArgsConstructor
public class SignController {

    private final SignService service;

    // ===== 외부 API 조회 =====
    @GetMapping("/external")
    public ResponseEntity<ApiResponse<SignPageDto>> externalList(
            @RequestParam(name = "pageNo", required = false) Integer pageNo,
            @RequestParam(name = "numOfRows", required = false) Integer numOfRows,
            @RequestParam(name = "page", required = false) Integer legacyPage,
            @RequestParam(name = "size", required = false) Integer legacySize
    ) {
        int p = (pageNo != null) ? pageNo : (legacyPage != null ? legacyPage : 1);
        int s = (numOfRows != null) ? numOfRows : (legacySize != null ? legacySize : 10);
        try {
            var data = service.externalList(p, s);
            return ApiResponse.success(SuccessStatus.FETCHED, data);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                return ApiResponse.error(ErrorStatus.EXTERNAL_QUOTA);
            }
            throw e;
        }
    }

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
        try {
            var data = service.externalSearch(keyword, p, s);
            return ApiResponse.success(SuccessStatus.FETCHED, data);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                return ApiResponse.error(ErrorStatus.EXTERNAL_QUOTA);
            }
            throw e;
        }
    }

    // ===== DB 조회 =====
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SignEntry>>> listFromDb(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var data = service.listFromDb(page, size);
        return ApiResponse.success(SuccessStatus.FETCHED, data);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<SignEntry>>> searchFromDb(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var data = service.searchFromDb(keyword, page, size);
        return ApiResponse.success(SuccessStatus.FETCHED, data);
    }

    // ===== 상세보기 (인증 선택) =====
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SignDetailDto>> getDetailById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        var dto = service.getDetailById(id, userId);
        return ApiResponse.success(SuccessStatus.FETCHED, dto);
    }

    @GetMapping("/by-local")
    public ResponseEntity<ApiResponse<SignDetailDto>> getDetailByLocalId(
            @RequestParam String localId,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        var dto = service.getDetailByLocalId(localId, userId);
        return ApiResponse.success(SuccessStatus.FETCHED, dto);
    }

    // ===== 즐겨찾기 (인증 필수) =====
    @PostMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Void>> addFavorite(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = requireUserId(authentication);
        service.addFavorite(userId, id);
        return ApiResponse.success(SuccessStatus.OK, null);
    }

    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = requireUserId(authentication);
        service.removeFavorite(userId, id);
        return ApiResponse.success(SuccessStatus.OK, null);
    }

    // ===== 마이페이지: 내가 찜한 수어 모아보기 (최신순) =====
    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<Page<SignFavoriteItemDto>>> listMyFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        Long userId = requireUserId(authentication);
        var data = service.listMyFavorites(userId, page, size);
        return ApiResponse.success(SuccessStatus.FETCHED, data);
    }

    // ===== 수집 =====
    @PostMapping("/ingest/all")
    public ResponseEntity<ApiResponse<IngestResultDto>> ingestAll(
            @RequestParam(defaultValue = "500") int size) {
        var res = service.ingestAll(size);
        return ApiResponse.success(SuccessStatus.OK, res);
    }

    @PostMapping("/ingest/all/parallel")
    public ResponseEntity<ApiResponse<IngestResultDto>> ingestAllParallel(
            @RequestParam(defaultValue = "500") int size) {
        var res = service.ingestAllParallel(size);
        return ApiResponse.success(SuccessStatus.OK, res);
    }

    // ===== 내부 유틸 =====
    private Long extractUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) return null;
        Object p = auth.getPrincipal();
        return (p instanceof Long l) ? l : null;
    }
    private Long requireUserId(Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null || userId <= 0) throw new IllegalArgumentException("인증이 필요합니다.");
        return userId;
    }
}
