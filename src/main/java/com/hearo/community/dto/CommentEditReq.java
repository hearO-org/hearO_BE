package com.hearo.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentEditReq(
        @NotBlank @Size(max = 2000) String content
) {}
