package com.hearo.community.repository;

import com.hearo.community.domain.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostImage i where i.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
