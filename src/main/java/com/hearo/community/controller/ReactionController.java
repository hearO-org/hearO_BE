package com.hearo.community.controller;

import com.hearo.community.dto.PostListRes;
import com.hearo.community.dto.ReactionRes;
import com.hearo.community.service.ReactionService;
import com.hearo.global.response.ApiResponse;
import com.hearo.global.response.SuccessStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService svc;

    /* ===== 게시물 좋아요 ===== */

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponse<ReactionRes>> likePost(Authentication authentication,
                                                             @PathVariable Long postId) {
        Long uid = requireUserId(authentication);
        var res = svc.likePost(uid, postId);
        return ApiResponse.success(SuccessStatus.UPDATED, res);
    }

    @DeleteMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponse<ReactionRes>> unlikePost(Authentication authentication,
                                                               @PathVariable Long postId) {
        Long uid = requireUserId(authentication);
        var res = svc.unlikePost(uid, postId);
        return ApiResponse.success(SuccessStatus.UPDATED, res);
    }

    /* ===== 댓글/대댓글 좋아요 ===== */

    @PostMapping("/posts/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<ReactionRes>> likeComment(Authentication authentication,
                                                                @PathVariable Long commentId) {
        Long uid = requireUserId(authentication);
        var res = svc.likeComment(uid, commentId);
        return ApiResponse.success(SuccessStatus.UPDATED, res);
    }

    @DeleteMapping("/posts/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<ReactionRes>> unlikeComment(Authentication authentication,
                                                                  @PathVariable Long commentId) {
        Long uid = requireUserId(authentication);
        var res = svc.unlikeComment(uid, commentId);
        return ApiResponse.success(SuccessStatus.UPDATED, res);
    }

    /* ===== 게시물 스크랩 ===== */

    @PostMapping("/posts/{postId}/scrap")
    public ResponseEntity<ApiResponse<ReactionRes>> scrapPost(Authentication authentication,
                                                              @PathVariable Long postId) {
        Long uid = requireUserId(authentication);
        var res = svc.scrapPost(uid, postId);
        return ApiResponse.success(SuccessStatus.UPDATED, res);
    }

    @DeleteMapping("/posts/{postId}/scrap")
    public ResponseEntity<ApiResponse<ReactionRes>> unscrapPost(Authentication authentication,
                                                                @PathVariable Long postId) {
        Long uid = requireUserId(authentication);
        var res = svc.unscrapPost(uid, postId);
        return ApiResponse.success(SuccessStatus.UPDATED, res);
    }

    /** 내 스크랩 목록 (스크랩한 시각 최신순) */
    @GetMapping("/posts/scrap/mine")
    public ResponseEntity<ApiResponse<PostListRes>> myScraps(Authentication authentication,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        Long uid = requireUserId(authentication);
        Page<com.hearo.community.dto.PostRes> p = svc.listMyScraps(uid, page, size);
        return ApiResponse.success(SuccessStatus.FETCHED, PostListRes.of(p));
    }

    private Long requireUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null || !(auth.getPrincipal() instanceof Long uid) || uid <= 0)
            throw new IllegalArgumentException("인증이 필요합니다.");
        return uid;
    }
}
