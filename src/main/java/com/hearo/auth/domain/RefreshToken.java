package com.hearo.auth.domain;

import com.hearo.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long userId;
    @Column(nullable = false, unique = true, length = 64)
    private String jti;                               // 토큰 식별자
    @Column(nullable = false) private int tokenVersion;
    @Column(nullable = false) private Instant expiresAt;

    @Column(nullable = false) private boolean revoked;
    private String replacedBy;                        // 회전된 새 jti

    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    public void revoke(String replacedByJti) { this.revoked = true; this.replacedBy = replacedByJti; }
    public void revokeOnly() { this.revoked = true; }
}
