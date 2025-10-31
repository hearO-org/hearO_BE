package com.hearo.community.repository;

import com.hearo.community.domain.PostScrap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostScrapRepository extends JpaRepository<PostScrap, Long> {
    boolean existsByUser_IdAndPost_Id(Long userId, Long postId);
    Optional<PostScrap> findByUser_IdAndPost_Id(Long userId, Long postId);
    void deleteByUser_IdAndPost_Id(Long userId, Long postId);
}
