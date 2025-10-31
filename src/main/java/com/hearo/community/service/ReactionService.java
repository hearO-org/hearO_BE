package com.hearo.community.service;

import com.hearo.community.domain.*;
import com.hearo.community.dto.ReactionRes;
import com.hearo.community.repository.*;
import com.hearo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReactionService {

    private final PostRepository posts;
    private final CommentRepository comments;
    private final PostLikeRepository postLikes;
    private final CommentLikeRepository commentLikes;
    private final PostScrapRepository postScraps;
    private final UserRepository users;

    /* ===== 게시물 좋아요 ===== */

    public ReactionRes likePost(Long userId, Long postId) {
        var p = posts.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));
        var u = users.findById(userId).orElseThrow();

        if (!postLikes.existsByUser_IdAndPost_Id(userId, postId)) {
            try {
                postLikes.save(PostLike.of(u, p));
            } catch (DataIntegrityViolationException ignore) {
                // 동시성으로 인한 Unique 충돌은 멱등 처리: 이미 좋아요 상태로 간주
            }
        }
        long cnt = postLikes.countByPost_Id(postId);
        return new ReactionRes(true, cnt);
    }

    public ReactionRes unlikePost(Long userId, Long postId) {
        posts.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));
        postLikes.deleteByUser_IdAndPost_Id(userId, postId);
        long cnt = postLikes.countByPost_Id(postId);
        return new ReactionRes(false, cnt);
    }

    /* ===== 댓글/대댓글 좋아요 ===== */

    public ReactionRes likeComment(Long userId, Long commentId) {
        var c = comments.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (c.isDeleted()) throw new IllegalArgumentException("삭제된 댓글입니다.");

        var u = users.findById(userId).orElseThrow();
        if (!commentLikes.existsByUser_IdAndComment_Id(userId, commentId)) {
            try {
                commentLikes.save(CommentLike.of(u, c));
            } catch (DataIntegrityViolationException ignore) {
                // 동시성으로 인한 Unique 충돌은 멱등 처리
            }
        }
        long cnt = commentLikes.countByComment_Id(commentId);
        return new ReactionRes(true, cnt);
    }

    public ReactionRes unlikeComment(Long userId, Long commentId) {
        var c = comments.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (c.isDeleted()) throw new IllegalArgumentException("삭제된 댓글입니다.");

        commentLikes.deleteByUser_IdAndComment_Id(userId, commentId);
        long cnt = commentLikes.countByComment_Id(commentId);
        return new ReactionRes(false, cnt);
    }

    /* ===== 게시물 스크랩 ===== */

    public ReactionRes scrapPost(Long userId, Long postId) {
        var p = posts.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));
        var u = users.findById(userId).orElseThrow();

        if (!postScraps.existsByUser_IdAndPost_Id(userId, postId)) {
            try {
                postScraps.save(PostScrap.of(u, p));
            } catch (DataIntegrityViolationException ignore) {
                // 동시성으로 인한 Unique 충돌은 멱등 처리
            }
        }
        // 스크랩은 카운트 보지 않기로 했으므로 0 고정
        return new ReactionRes(true, 0);
    }

    public ReactionRes unscrapPost(Long userId, Long postId) {
        posts.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));
        postScraps.deleteByUser_IdAndPost_Id(userId, postId);
        return new ReactionRes(false, 0);
    }

    /** 내 스크랩 목록 (스크랩한 시각 최신순) */
    public Page<com.hearo.community.dto.PostRes> listMyScraps(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return posts.findScrappedPosts(userId, pageable).map(com.hearo.community.dto.PostRes::of);
    }
}
