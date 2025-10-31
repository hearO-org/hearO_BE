package com.hearo.signlanguage.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SignFavoriteItemDto {
    private Long id;               // sign entry id
    private String localId;
    private String title;
    private String videoUrl;
    private String thumbnailUrl;
    private String signDescription;
    private List<String> images;   // imagesCsv → List
    private String sourceUrl;
    private String collectionDb;
    private String categoryType;
    private Integer viewCount;

    private LocalDateTime favoritedAt; // 내가 찜한 시각
}
