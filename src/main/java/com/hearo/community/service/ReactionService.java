package com.hearo.community.service;

import com.hearo.community.domain.*;
import com.hearo.community.dto.PostRes;
import com.hearo.community.dto.ReactionRes;
import com.hearo.community.repository.*;
import com.hearo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
    @Transactional(readOnly = true)
    public Page<PostRes> listMyScraps(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> p = posts.findScrappedPosts(userId, pageable);
        return attachReactions(p, userId);
    }

    /** 내가 좋아요한 게시물 목록 (좋아요한 시각 최신순) */
    @Transactional(readOnly = true)
    public Page<PostRes> listMyLikedPosts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> p = posts.findLikedPosts(userId, pageable);
        return attachReactions(p, userId);
    }

    /** 좋아요/스크랩 플래그 + likeCount 붙여주는 공통 로직 */
    private Page<PostRes> attachReactions(Page<Post> page, Long userId) {
        var content = page.getContent();
        if (content.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), page.getPageable(), page.getTotalElements());
        }

        var ids = content.stream().map(Post::getId).toList();

        // 좋아요 수 집계
        Map<Long, Long> likeCountMap = new HashMap<>();
        for (Object[] r : postLikes.countByPostIds(ids)) {
            likeCountMap.put((Long) r[0], (Long) r[1]); // r[0]=postId, r[1]=count
        }

        Map<Long, Boolean> likedMap = new HashMap<>();
        Map<Long, Boolean> scrappedMap = new HashMap<>();

        // 현재 사용자 기준으로 liked/scrapped 플래그 세팅
        for (Long pid : postLikes.findLikedPostIds(userId, ids)) {
            likedMap.put(pid, Boolean.TRUE);
        }
        for (Long pid : postScraps.findScrappedPostIds(userId, ids)) {
            scrappedMap.put(pid, Boolean.TRUE);
        }

        var mapped = content.stream()
                .map(p -> {
                    long likeCnt = likeCountMap.getOrDefault(p.getId(), 0L);
                    boolean liked = Boolean.TRUE.equals(likedMap.get(p.getId()));
                    boolean scrapped = Boolean.TRUE.equals(scrappedMap.get(p.getId()));
                    return PostRes.of(p, likeCnt, liked, scrapped);
                })
                .toList();

        return new PageImpl<>(mapped, page.getPageable(), page.getTotalElements());
    }
}
