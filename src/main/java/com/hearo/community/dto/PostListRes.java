package com.hearo.community.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PostListRes(
        long totalElements,
        int totalPages,
        int page,
        int size,
        List<PostRes> content
) {
    public static PostListRes of(Page<PostRes> page){
        return new PostListRes(
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.getContent()
        );
    }
}
