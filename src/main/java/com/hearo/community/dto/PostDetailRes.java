package com.hearo.community.dto;

import com.hearo.community.domain.Post;
import com.hearo.community.domain.PostCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record PostDetailRes(
        Long id,
        Long authorId,
        String authorNickname,
        String title,
        String content,
        PostCategory category,
        String visibility,
        List<String> images,
        Set<String> tags,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        long likeCount,     // 단건 조회에 포함
        boolean liked,      // 로그인 사용자 기준
        boolean scrapped    // 로그인 사용자 기준
) {
    public static PostDetailRes of(Post p, long likeCount, boolean liked, boolean scrapped){
        List<String> imgs = p.getImages().stream().map(i -> i.getUrl()).toList();
        return new PostDetailRes(
                p.getId(),
                p.getAuthor().getId(),
                p.getAuthor().getNickname(),
                p.getTitle(),
                p.getContent(),
                p.getCategory(),
                p.getVisibility().name(),
                imgs,
                p.getTags(),
                p.getCreatedAt(),
                p.getModifiedAt(),
                likeCount,
                liked,
                scrapped
        );
    }
}
