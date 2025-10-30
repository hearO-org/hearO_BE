package com.hearo.community.repository;

import com.hearo.community.domain.Comment;
import com.hearo.community.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByPostAndParentIsNullAndDeletedFalse(Post post, Pageable pageable); // 댓글

    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByParentAndDeletedFalse(Comment parent, Pageable pageable); // 대댓글
}
