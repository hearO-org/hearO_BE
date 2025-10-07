package com.hearo.signlanguage.domain;

import com.hearo.signlanguage.dto.SignItemDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sign_entries", indexes = {
        @Index(name = "idx_sign_local_id", columnList = "localId", unique = true),
        @Index(name = "idx_sign_title", columnList = "title")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SignEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String localId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String videoUrl;

    @Column(length = 1000)
    private String thumbnailUrl;

    @Lob @Column(columnDefinition = "TEXT")
    private String signDescription;

    @Lob @Column(columnDefinition = "TEXT")
    private String imagesCsv;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(length = 100)
    private String collectionDb;

    @Column(length = 100)
    private String categoryType;

    private Integer viewCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { this.createdAt = LocalDateTime.now(); this.updatedAt = this.createdAt; }
    @PreUpdate  void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public void updateFrom(SignItemDto dto) {
        this.title = dto.getTitle();
        this.videoUrl = dto.getVideoUrl();
        this.thumbnailUrl = dto.getThumbnailUrl();
        this.signDescription = dto.getSignDescription();
        this.imagesCsv = String.join(",", safeList(dto.getImages()));
        this.sourceUrl = dto.getSourceUrl();
        this.collectionDb = dto.getCollectionDb();
        this.categoryType = dto.getCategoryType();
        this.viewCount = dto.getViewCount();
    }

    private static List<String> safeList(List<String> in) { return (in == null) ? List.of() : in; }
}
