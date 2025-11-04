package com.hearo.community.dto;

public record ReactionRes(
        boolean likedOrScrapped, // true=추가됨, false=해제됨
        long count               // 현재 카운트 (likeCount 등)
) {}
