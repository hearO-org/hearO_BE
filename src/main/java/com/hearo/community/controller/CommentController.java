package com.hearo.community.controller;

import com.hearo.community.dto.CommentCreateReq;
import com.hearo.community.dto.CommentEditReq;
import com.hearo.community.dto.CommentRes;
import com.hearo.community.service.CommentService;
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
public class CommentController {

    private final CommentService svc;

    /** 게시물의 댓글 목록 */
    @GetMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<Page<CommentRes>>> list(@PathVariable Long postId,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(SuccessStatus.FETCHED, svc.listComments(postId, page, size));
    }

    /** 특정 댓글의 대댓글 목록 */
    @GetMapping("/{postId}/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<Page<CommentRes>>> replies(@PathVariable Long postId,
                                                                 @PathVariable Long commentId,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(SuccessStatus.FETCHED, svc.listReplies(commentId, page, size));
    }

    /** 내가 작성한 댓글 목록 */
    @GetMapping("/comments/me")
    public ResponseEntity<ApiResponse<Page<CommentRes>>> myComments(Authentication authentication,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        Long uid = requireUserId(authentication);
        return ApiResponse.success(SuccessStatus.FETCHED, svc.listMyComments(uid, page, size));
    }

    /** 댓글 작성 */
    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<Long>> write(Authentication authentication,
                                                   @PathVariable Long postId,
                                                   @RequestBody @Valid CommentCreateReq req) {
        Long uid = requireUserId(authentication);
        return ApiResponse.success(SuccessStatus.CREATED, svc.write(uid, postId, req));
    }

    /** 대댓글 작성 */
    @PostMapping("/{postId}/comments/{parentId}/replies")
    public ResponseEntity<ApiResponse<Long>> reply(Authentication authentication,
                                                   @PathVariable Long postId,
                                                   @PathVariable Long parentId,
                                                   @RequestBody @Valid CommentCreateReq req) {
        Long uid = requireUserId(authentication);
        return ApiResponse.success(SuccessStatus.CREATED, svc.reply(uid, postId, parentId, req));
    }

    /** 댓글 수정 */
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> edit(Authentication authentication,
                                                  @PathVariable Long commentId,
                                                  @RequestBody @Valid CommentEditReq req) {
        Long uid = requireUserId(authentication);
        svc.edit(uid, commentId, req);
        return ApiResponse.success(SuccessStatus.UPDATED);
    }

    /** 댓글 삭제 */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication,
                                                    @PathVariable Long commentId) {
        Long uid = requireUserId(authentication);
        svc.delete(uid, commentId);
        return ApiResponse.success(SuccessStatus.DELETED);
    }

    private Long requireUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null || !(auth.getPrincipal() instanceof Long uid) || uid <= 0) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }
        return uid;
    }
}
