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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    private final PostRepository posts;
    private final PostLikeRepository postLikes;
    private final PostScrapRepository postScraps;

    public Page<PostRes> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return posts.findAllByDeletedFalse(pageable).map(PostRes::of);
    }

    public Page<PostRes> search(String q, PostCategory category, String tag, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Post> spec = Specification.where((root, cq, cb) -> cb.isFalse(root.get("deleted")));

        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim() + "%";
            spec = spec.and((root, cq, cb) ->
                    cb.or(cb.like(root.get("title"), like),
                            cb.like(root.get("content"), like))
            );
        }
        if (category != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("category"), category));
        }
        if (tag != null && !tag.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.isMember(tag.trim().toLowerCase(), root.get("tags")));
        }
        return posts.findAll(spec, pageable).map(PostRes::of);
    }

    /** 단건 상세: likeCount(+ 로그인 시 liked/scrapped) 포함 */
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
}
