package com.hearo.signlanguage.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SignDetailDto {
    private Long id;
    private String localId;
    private String title;
    private String videoUrl;
    private String thumbnailUrl;
    private String signDescription;
    private List<String> images;
    private String sourceUrl;
    private String collectionDb;
    private String categoryType;
    private Integer viewCount;

    private boolean favorite;      // 현재 사용자 기준 즐겨찾기 여부
    private long favoriteCount;    // 전체 즐겨찾기 수
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}
