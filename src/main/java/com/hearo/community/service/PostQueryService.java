package com.hearo.community.service;

import com.hearo.community.domain.Post;
import com.hearo.community.domain.PostCategory;
import com.hearo.community.dto.PostDetailRes;
import com.hearo.community.dto.PostRes;
import com.hearo.community.repository.PostLikeRepository;
import com.hearo.community.repository.PostRepository;
import com.hearo.community.repository.PostScrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    private final PostRepository posts;
    private final PostLikeRepository postLikes;
    private final PostScrapRepository postScraps;

    public Page<PostRes> list(int page, int size, Long userIdOrNull) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> p = posts.findAllByDeletedFalse(pageable);
        return attachReactions(p, userIdOrNull);
    }

    public Page<PostRes> search(String q, PostCategory category, String tag, int page, int size, Long userIdOrNull) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Post> spec = Specification.where((root, cq, cb) -> cb.isFalse(root.get("deleted")));
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim() + "%";
            spec = spec.and((root, cq, cb) ->
                    cb.or(cb.like(root.get("title"), like),
                            cb.like(root.get("content"), like)));
        }
        if (category != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("category"), category));
        }
        if (tag != null && !tag.isBlank()) {
            String normalized = tag.trim().toLowerCase();
            spec = spec.and((root, cq, cb) -> cb.isMember(normalized, root.get("tags")));
        }

        Page<Post> p = posts.findAll(spec, pageable);
        return attachReactions(p, userIdOrNull);
    }

    public PostDetailRes getDetail(Long postId, Long userIdOrNull) {
        Post p = posts.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));

        long likeCount = postLikes.countByPost_Id(postId);
        boolean liked = false;
        boolean scrapped = false;

        if (userIdOrNull != null && userIdOrNull > 0) {
            liked = postLikes.existsByUser_IdAndPost_Id(userIdOrNull, postId);
            scrapped = postScraps.existsByUser_IdAndPost_Id(userIdOrNull, postId);
        }
        return PostDetailRes.of(p, likeCount, liked, scrapped);
    }

    private Page<PostRes> attachReactions(Page<Post> page, Long userIdOrNull) {
        List<Post> content = page.getContent();
        if (content.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), page.getPageable(), page.getTotalElements());
        }

        // 게시물 ID 목록
        List<Long> ids = content.stream().map(Post::getId).collect(Collectors.toList());

        // 좋아요 수 집계
        final Map<Long, Long> likeCountMap = new HashMap<>();
        for (Object[] r : postLikes.countByPostIds(ids)) {
            likeCountMap.put((Long) r[0], (Long) r[1]); // r[0]=postId, r[1]=count
        }

        // 람다에서 안전하게 쓰기 위해 '변수 재할당 금지' 패턴 사용
        final Map<Long, Boolean> likedMap    = new HashMap<>();
        final Map<Long, Boolean> scrappedMap = new HashMap<>();

        if (userIdOrNull != null && userIdOrNull > 0) {
            for (Long pid : postLikes.findLikedPostIds(userIdOrNull, ids)) {
                likedMap.put(pid, Boolean.TRUE);
            }
            for (Long pid : postScraps.findScrappedPostIds(userIdOrNull, ids)) {
                scrappedMap.put(pid, Boolean.TRUE);
            }
        }

        // 목록 매핑
        List<PostRes> mapped = content.stream()
                .map(p -> {
                    long likeCnt   = likeCountMap.getOrDefault(p.getId(), 0L);
                    boolean liked  = (userIdOrNull != null) && Boolean.TRUE.equals(likedMap.get(p.getId()));
                    boolean scrapd = (userIdOrNull != null) && Boolean.TRUE.equals(scrappedMap.get(p.getId()));
                    return PostRes.of(p, likeCnt, liked, scrapd);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(mapped, page.getPageable(), page.getTotalElements());
    }
}
