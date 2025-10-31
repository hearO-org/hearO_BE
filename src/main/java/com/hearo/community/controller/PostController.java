package com.hearo.community.controller;

import com.hearo.community.domain.PostCategory;
import com.hearo.community.dto.PostCreateReq;
import com.hearo.community.dto.PostEditReq;
import com.hearo.community.dto.PostListRes;
import com.hearo.community.dto.PostDetailRes;
import com.hearo.community.dto.PostRes;
import com.hearo.community.service.PostCommandService;
import com.hearo.community.service.PostQueryService;
import com.hearo.global.response.ApiResponse;
import com.hearo.global.response.SuccessStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/community/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostCommandService cmd;
    private final PostQueryService query;

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(Authentication authentication,
                                                    @RequestBody @Valid PostCreateReq req) {
        Long uid = requireUserId(authentication);
        return ApiResponse.success(SuccessStatus.CREATED, cmd.create(uid, req));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> edit(Authentication authentication,
                                                  @PathVariable Long postId,
                                                  @RequestBody @Valid PostEditReq req) {
        Long uid = requireUserId(authentication);
        cmd.edit(uid, postId, req);
        return ApiResponse.success(SuccessStatus.UPDATED);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication,
                                                    @PathVariable Long postId) {
        Long uid = requireUserId(authentication);
        cmd.delete(uid, postId);
        return ApiResponse.success(SuccessStatus.DELETED);
    }

    /** 단건 조회: likeCount(+ liked/scrapped) 포함 */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailRes>> get(Authentication authentication,
                                                          @PathVariable Long postId) {
        Long uid = optionalUserId(authentication);
        return ApiResponse.success(SuccessStatus.FETCHED, query.getDetail(postId, uid));
    }

    /** 전체 목록: likeCount/liked/scrapped 포함 */
    @GetMapping
    public ResponseEntity<ApiResponse<PostListRes>> list(Authentication authentication,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        Long uid = optionalUserId(authentication);
        Page<PostRes> p = query.list(page, size, uid);
        return ApiResponse.success(SuccessStatus.FETCHED, PostListRes.of(p));
    }

    /** 검색: likeCount/liked/scrapped 포함 */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PostListRes>> search(Authentication authentication,
                                                           @RequestParam(required = false) String q,
                                                           @RequestParam(required = false) PostCategory category,
                                                           @RequestParam(required = false) String tag,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        Long uid = optionalUserId(authentication);
        Page<PostRes> p = query.search(q, category, tag, page, size, uid);
        return ApiResponse.success(SuccessStatus.FETCHED, PostListRes.of(p));
    }

    /* ===== 공통 유틸 ===== */
    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long uid) || uid <= 0)
            throw new IllegalArgumentException("인증이 필요합니다.");
        return uid;
    }
    private Long optionalUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Long uid && uid > 0) return uid;
        return null;
    }
}
