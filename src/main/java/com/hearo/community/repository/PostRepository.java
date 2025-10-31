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

    // images + tags 모두 로딩 (tags는 @ElementCollection 이라 LAZY 초기화 이슈 방지)
    @EntityGraph(attributePaths = {"images", "tags"})
    Optional<Post> findByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = {"images", "tags"})
    Page<Post> findAllByDeletedFalse(Pageable pageable);

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
}
