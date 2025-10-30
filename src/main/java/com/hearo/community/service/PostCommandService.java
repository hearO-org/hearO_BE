package com.hearo.community.service;

import com.hearo.community.domain.Post;
import com.hearo.community.domain.PostImage;
import com.hearo.community.dto.PostCreateReq;
import com.hearo.community.dto.PostEditReq;
import com.hearo.community.repository.PostRepository;
import com.hearo.user.domain.User;
import com.hearo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostCommandService {

    private final PostRepository posts;
    private final UserRepository users;

    public Long create(Long authorId, PostCreateReq req) {
        User author = users.findById(authorId).orElseThrow();

        Post p = Post.create(author, req.title(), req.content(), req.category(), req.visibility());
        posts.save(p);

        // images
        List<String> imgs = Optional.ofNullable(req.imageUrls()).orElse(List.of());
        if (imgs.size() > 5) throw new IllegalArgumentException("이미지는 최대 5장까지 업로드할 수 있습니다.");
        int ord = 1;
        for (String url : imgs) {
            p.getImages().add(PostImage.of(p, url, ord++));
        }

        // tags (소문자 정규화)
        if (req.tags() != null) {
            req.tags().forEach(t -> p.getTags().add(t.trim().toLowerCase()));
        }

        return p.getId();
    }

    public void edit(Long editorId, Long postId, PostEditReq req) {
        Post p = posts.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));
        if (!p.getAuthor().getId().equals(editorId))
            throw new SecurityException("수정 권한이 없습니다.");

        p.edit(req.title(), req.content(), req.category(), req.visibility());

        p.getImages().clear();
        int ord = 1;
        for (String url : Optional.ofNullable(req.imageUrls()).orElse(List.of())) {
            p.getImages().add(PostImage.of(p, url, ord++));
        }

        p.getTags().clear();
        if (req.tags() != null) req.tags().forEach(t -> p.getTags().add(t.trim().toLowerCase()));
    }

    public void delete(Long userId, Long postId) {
        Post p = posts.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));
        if (!p.getAuthor().getId().equals(userId))
            throw new SecurityException("삭제 권한이 없습니다.");
        p.setDeleted(true); // soft delete
    }
}
