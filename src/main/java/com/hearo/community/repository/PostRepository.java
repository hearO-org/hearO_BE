package com.hearo.community.repository;

import com.hearo.community.domain.Post;
import com.hearo.community.domain.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    @EntityGraph(attributePaths = {"images"})
    Optional<Post> findByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = {"images"})
    Page<Post> findAllByDeletedFalse(Pageable pageable);

    boolean existsByIdAndAuthor_Id(Long postId, Long authorId);

    long countByCategoryAndDeletedFalse(PostCategory category);
}
