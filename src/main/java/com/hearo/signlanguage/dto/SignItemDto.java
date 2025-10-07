package com.hearo.signlanguage.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SignItemDto {
    private String title;
    private String localId;
    private String videoUrl;
    private String thumbnailUrl;
    private String signDescription;
    private List<String> images;
    private String sourceUrl;
    private String collectionDb;
    private String categoryType;
    private Integer viewCount;
}
