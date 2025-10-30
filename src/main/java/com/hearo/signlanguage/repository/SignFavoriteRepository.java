package com.hearo.signlanguage.repository;

import com.hearo.signlanguage.domain.SignFavorite;
import com.hearo.signlanguage.dto.SignFavoriteRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SignFavoriteRepository extends JpaRepository<SignFavorite, Long> {

    boolean existsByUserIdAndSignEntryId(Long userId, Long signEntryId);

    long countBySignEntryId(Long signEntryId);

    void deleteByUserIdAndSignEntryId(Long userId, Long signEntryId);

    // 마이페이지 목록: 내가 찜한 수어 + 찜한 시각 (최신순)
    @Query("""
      select new com.hearo.signlanguage.dto.SignFavoriteRow(
         s.id, s.localId, s.title, s.videoUrl, s.thumbnailUrl, s.signDescription,
         s.imagesCsv, s.sourceUrl, s.collectionDb, s.categoryType, s.viewCount,
         f.createdAt
      )
      from com.hearo.signlanguage.domain.SignFavorite f
      join f.signEntry s
      where f.userId = :userId
      order by f.createdAt desc
    """)
    Page<SignFavoriteRow> findFavoriteRows(Long userId, Pageable pageable);
}
