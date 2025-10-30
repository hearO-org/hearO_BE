package com.hearo.community.service;

import com.hearo.community.domain.Post;
import com.hearo.community.domain.PostCategory;
import com.hearo.community.dto.PostRes;
import com.hearo.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    private final PostRepository posts;

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
                    cb.or(
                        cb.like(root.get("title"), like),
                        cb.like(root.get("content"), like)
                    )
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

    public PostRes get(Long id) {
        Post p = posts.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));
        return PostRes.of(p);
    }
}
