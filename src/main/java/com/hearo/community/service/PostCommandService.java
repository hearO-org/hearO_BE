package com.hearo.community.service;

import com.hearo.community.domain.Post;
import com.hearo.community.domain.PostImage;
import com.hearo.community.dto.PostCreateReq;
import com.hearo.community.dto.PostEditReq;
import com.hearo.community.repository.PostRepository;
import com.hearo.user.domain.User;
import com.hearo.user.repository.UserRepository;
import com.hearo.community.repository.CommentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    private final CommentRepository comments;

    @PersistenceContext
    private EntityManager em;

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
        // 여기서 images + tags 가 함께 로딩됨(PostRepository에서 EntityGraph 설정)
        Post p = posts.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));
        if (!p.getAuthor().getId().equals(editorId))
            throw new SecurityException("수정 권한이 없습니다.");

        // 1) 본문/메타 수정
        p.edit(req.title(), req.content(), req.category(), req.visibility());

        // 2) 이미지 교체
        //    - 먼저 기존 이미지 orphanRemoval로 제거
        //    - 그 즉시 flush() 해서 (post_id, ordering) 유니크 제약을 완전히 비워둔다
        p.getImages().clear();
        em.flush(); // 유니크 충돌(1-1) 원천 차단

        int ord = 1;
        for (String url : Optional.ofNullable(req.imageUrls()).orElse(List.of())) {
            if (ord > 5) throw new IllegalArgumentException("이미지는 최대 5장까지 업로드할 수 있습니다.");
            p.getImages().add(PostImage.of(p, url, ord++));
        }

        // 3) 태그 교체
        //    - 위에서 tags를 이미 로딩했기 때문에 LazyInitializationException 없음
        p.getTags().clear();
        if (req.tags() != null) {
            req.tags().forEach(t -> p.getTags().add(t.trim().toLowerCase()));
        }
    }

    public void delete(Long userId, Long postId) {
        var p = posts.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));
        if (!p.getAuthor().getId().equals(userId))
            throw new SecurityException("삭제 권한이 없습니다.");

        p.setDeleted(true);                       // 게시물 soft delete
        comments.softDeleteAllByPostId(postId);   // 댓글/대댓글 전부 soft delete
    }
}
