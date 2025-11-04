package com.hearo.signlanguage.repository;

import com.hearo.signlanguage.dto.SignItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SignEntryBulkRepository {

    private final JdbcTemplate jdbc;

    private static final String UPSERT_SQL = """
        INSERT INTO sign_entries
          (local_id, title, video_url, thumbnail_url, sign_description, images_csv,
           source_url, collection_db, category_type, view_count, created_at, modified_at)
        VALUES
          (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
          title = VALUES(title),
          video_url = VALUES(video_url),
          thumbnail_url = VALUES(thumbnail_url),
          sign_description = VALUES(sign_description),
          images_csv = VALUES(images_csv),
          source_url = VALUES(source_url),
          collection_db = VALUES(collection_db),
          category_type = VALUES(category_type),
          view_count = VALUES(view_count),
          modified_at = NOW()
        """;

    public int[] upsertBatch(List<SignItemDto> items) {
        if (items == null || items.isEmpty()) return new int[0];

        return jdbc.batchUpdate(UPSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SignItemDto it = items.get(i);

                ps.setString(1,  it.getLocalId());
                ps.setString(2,  it.getTitle());
                ps.setString(3,  it.getVideoUrl());
                ps.setString(4,  it.getThumbnailUrl());
                ps.setString(5,  it.getSignDescription());

                String imagesCsv = (it.getImages() == null || it.getImages().isEmpty())
                        ? "" : String.join(",", it.getImages());
                ps.setString(6,  imagesCsv);

                ps.setString(7,  it.getSourceUrl());
                ps.setString(8,  it.getCollectionDb());
                ps.setString(9,  it.getCategoryType());

                if (it.getViewCount() == null) {
                    ps.setNull(10, Types.INTEGER);
                } else {
                    ps.setInt(10, it.getViewCount());
                }
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });
    }
}
