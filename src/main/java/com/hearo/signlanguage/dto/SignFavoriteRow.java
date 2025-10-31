package com.hearo.signlanguage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SignFavoriteRow {
    public Long id;
    public String localId;
    public String title;
    public String videoUrl;
    public String thumbnailUrl;
    public String signDescription;
    public String imagesCsv;      // 나중에 List로 변환
    public String sourceUrl;
    public String collectionDb;
    public String categoryType;
    public Integer viewCount;
    public LocalDateTime favoritedAt;
}
