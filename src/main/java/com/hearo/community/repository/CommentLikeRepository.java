package com.hearo.community.repository;

import com.hearo.community.domain.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    boolean existsByUser_IdAndComment_Id(Long userId, Long commentId);
    Optional<CommentLike> findByUser_IdAndComment_Id(Long userId, Long commentId);
    long countByComment_Id(Long commentId);
    void deleteByUser_IdAndComment_Id(Long userId, Long commentId);
}
