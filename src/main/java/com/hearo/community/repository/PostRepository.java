package com.hearo.community.repository;

import com.hearo.community.domain.Post;
import com.hearo.community.domain.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    /**
     * 단건 조회
     */
    @EntityGraph(attributePaths = {"images"})
    Optional<Post> findByIdAndDeletedFalse(Long id);

    /**
     * 전체 목록 조회
     */
    @EntityGraph(attributePaths = {"images"})
    Page<Post> findAllByDeletedFalse(Pageable pageable);

    /**
     * 특정 사용자가 작성한 게시글 목록
     */
    @EntityGraph(attributePaths = {"images"})
    Page<Post> findByAuthor_IdAndDeletedFalse(Long authorId, Pageable pageable);

    boolean existsByIdAndAuthor_Id(Long postId, Long authorId);

    long countByCategoryAndDeletedFalse(PostCategory category);

    // 내 스크랩 목록 (스크랩한 시각 최신순)
    @EntityGraph(attributePaths = {"images"})
    @Query(value = """
            select p
            from PostScrap s
            join s.post p
            where s.user.id = :uid and p.deleted = false
            order by s.createdAt desc
            """,
            countQuery = """
            select count(s)
            from PostScrap s
            join s.post p
            where s.user.id = :uid and p.deleted = false
            """)
    Page<Post> findScrappedPosts(@Param("uid") Long userId, Pageable pageable);

    // 내가 좋아요한 게시물 목록 (좋아요한 시각 최신순)
    @EntityGraph(attributePaths = {"images"})
    @Query(value = """
            select p
            from PostLike l
            join l.post p
            where l.user.id = :uid and p.deleted = false
            order by l.createdAt desc
            """,
            countQuery = """
            select count(l)
            from PostLike l
            join l.post p
            where l.user.id = :uid and p.deleted = false
            """)
    Page<Post> findLikedPosts(@Param("uid") Long userId, Pageable pageable);
}
