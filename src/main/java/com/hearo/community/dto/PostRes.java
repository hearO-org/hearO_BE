package com.hearo.community.dto;

import com.hearo.community.domain.Post;
import com.hearo.community.domain.PostCategory;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record PostRes(
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
        long likeCount,
        boolean liked,
        boolean scrapped
) {
    public static PostRes of(Post p){
        List<String> imgs = p.getImages().stream()
                .map(i -> i.getUrl())
                .toList();

        Set<String> tagsCopy = new LinkedHashSet<>(p.getTags());

        return new PostRes(
                p.getId(),
                p.getAuthor().getId(),
                p.getAuthor().getNickname(),
                p.getTitle(),
                p.getContent(),
                p.getCategory(),
                p.getVisibility().name(),
                imgs,
                tagsCopy,
                p.getCreatedAt(),
                p.getModifiedAt(),
                0L,
                false,
                false
        );
    }

    public static PostRes of(Post p, long likeCount, boolean liked, boolean scrapped){
        List<String> imgs = p.getImages().stream()
                .map(i -> i.getUrl())
                .toList();

        Set<String> tagsCopy = new LinkedHashSet<>(p.getTags());

        return new PostRes(
                p.getId(),
                p.getAuthor().getId(),
                p.getAuthor().getNickname(),
                p.getTitle(),
                p.getContent(),
                p.getCategory(),
                p.getVisibility().name(),
                imgs,
                tagsCopy,
                p.getCreatedAt(),
                p.getModifiedAt(),
                likeCount,
                liked,
                scrapped
        );
    }
}
