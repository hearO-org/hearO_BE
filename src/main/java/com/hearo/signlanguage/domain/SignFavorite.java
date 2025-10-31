package com.hearo.signlanguage.domain;

import com.hearo.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sign_favorites",
        uniqueConstraints = @UniqueConstraint(name = "uk_sign_fav_user_sign", columnNames = {"user_id", "sign_entry_id"}),
        indexes = {
                @Index(name = "idx_sign_fav_user", columnList = "user_id"),
                @Index(name = "idx_sign_fav_sign", columnList = "sign_entry_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SignFavorite extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sign_entry_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sign_fav_sign_entry"))
    private SignEntry signEntry;

    public static SignFavorite of(Long userId, SignEntry entry) {
        return SignFavorite.builder()
                .userId(userId)
                .signEntry(entry)
                .build();
    }
}
