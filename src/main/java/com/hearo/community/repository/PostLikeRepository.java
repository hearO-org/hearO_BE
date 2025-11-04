package com.hearo.community.repository;

import com.hearo.community.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByUser_IdAndPost_Id(Long userId, Long postId);
    Optional<PostLike> findByUser_IdAndPost_Id(Long userId, Long postId);
    long countByPost_Id(Long postId);
    void deleteByUser_IdAndPost_Id(Long userId, Long postId);

    // [배치] 여러 게시물의 좋아요 수
    @Query("select pl.post.id, count(pl) from PostLike pl where pl.post.id in :ids group by pl.post.id")
    List<Object[]> countByPostIds(@Param("ids") List<Long> postIds);

    // [배치] 특정 사용자가 좋아요한 게시물 ID 집합
    @Query("select pl.post.id from PostLike pl where pl.user.id = :uid and pl.post.id in :ids")
    List<Long> findLikedPostIds(@Param("uid") Long userId, @Param("ids") List<Long> postIds);
}
