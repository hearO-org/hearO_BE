package com.hearo.community.controller;

import com.hearo.community.domain.PostCategory;
import com.hearo.community.dto.PostCreateReq;
import com.hearo.community.dto.PostEditReq;
import com.hearo.community.dto.PostListRes;
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

    /** 글 작성 */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(Authentication authentication,
                                                    @RequestBody @Valid PostCreateReq req) {
        Long uid = requireUserId(authentication);
        return ApiResponse.success(SuccessStatus.CREATED, cmd.create(uid, req));
    }

    /** 글 수정 */
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> edit(Authentication authentication,
                                                  @PathVariable Long postId,
                                                  @RequestBody @Valid PostEditReq req) {
        Long uid = requireUserId(authentication);
        cmd.edit(uid, postId, req);
        return ApiResponse.success(SuccessStatus.UPDATED);
    }

    /** 글 삭제 (soft delete) */
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication,
                                                    @PathVariable Long postId) {
        Long uid = requireUserId(authentication);
        cmd.delete(uid, postId);
        return ApiResponse.success(SuccessStatus.DELETED);
    }

    /** 단건 조회 */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostRes>> get(@PathVariable Long postId) {
        return ApiResponse.success(SuccessStatus.FETCHED, query.get(postId));
    }

    /** 전체 목록 */
    @GetMapping
    public ResponseEntity<ApiResponse<PostListRes>> list(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        Page<PostRes> p = query.list(page, size);
        return ApiResponse.success(SuccessStatus.FETCHED, PostListRes.of(p));
    }

    /** 검색 (q=키워드, category=카테고리, tag=태그) */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PostListRes>> search(@RequestParam(required = false) String q,
                                                           @RequestParam(required = false) PostCategory category,
                                                           @RequestParam(required = false) String tag,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        Page<PostRes> p = query.search(q, category, tag, page, size);
        return ApiResponse.success(SuccessStatus.FETCHED, PostListRes.of(p));
    }

    private Long requireUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null || !(auth.getPrincipal() instanceof Long uid) || uid <= 0) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }
        return uid;
    }
}
