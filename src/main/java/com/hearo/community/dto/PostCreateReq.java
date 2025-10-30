package com.hearo.community.dto;

import com.hearo.community.domain.Post;
import com.hearo.community.domain.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public record PostCreateReq(
        @NotBlank @Size(max=120) String title,
        @NotBlank @Size(max=5000) String content,
        PostCategory category,
        Post.Visibility visibility,
        @Size(max=5) List<@Pattern(regexp="^https?://.+") String> imageUrls,
        @Size(max=10) Set<@Size(min=1, max=32) String> tags
) {}
