package com.hearo.community.repository;

import com.hearo.community.domain.Comment;
import com.hearo.community.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByPostAndParentIsNullAndDeletedFalse(Post post, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByParentAndDeletedFalse(Comment parent, Pageable pageable);

    /** 댓글 단건(게시물/댓글이 모두 활성) */
    @Query("""
           select c
           from Comment c
           join fetch c.post p
           join fetch c.author a
           where c.id = :cid
             and c.deleted = false
             and p.deleted = false
           """)
    Optional<Comment> findActiveById(@Param("cid") Long commentId);

    /** 게시물 삭제 시 모든 댓글/대댓글 soft delete */
    @Modifying
    @Query("update Comment c set c.deleted = true where c.post.id = :pid and c.deleted = false")
    int softDeleteAllByPostId(@Param("pid") Long postId);

    /** 부모 댓글 삭제 시 해당 부모의 대댓글만 soft delete */
    @Modifying
    @Query("update Comment c set c.deleted = true where c.parent.id = :parentId and c.deleted = false")
    int softDeleteAllByParentId(@Param("parentId") Long parentId);
}
