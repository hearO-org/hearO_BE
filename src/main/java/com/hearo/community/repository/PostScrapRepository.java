package com.hearo.community.repository;

import com.hearo.community.domain.PostScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostScrapRepository extends JpaRepository<PostScrap, Long> {
    boolean existsByUser_IdAndPost_Id(Long userId, Long postId);
    Optional<PostScrap> findByUser_IdAndPost_Id(Long userId, Long postId);
    void deleteByUser_IdAndPost_Id(Long userId, Long postId);

    // [배치] 특정 사용자가 스크랩한 게시물 ID 집합
    @Query("select ps.post.id from PostScrap ps where ps.user.id = :uid and ps.post.id in :ids")
    List<Long> findScrappedPostIds(@Param("uid") Long userId, @Param("ids") List<Long> postIds);
}
