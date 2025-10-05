package com.hearo.user.domain;

import com.hearo.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true) private String email;    // LOCAL
    private String passwordHash;                    // LOCAL
    @Column(unique = true) private String kakaoId;  // KAKAO

    @Column(nullable = false) private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuthType authType;

    @Column(nullable = false) private Boolean enabled;

    @Column(nullable = false) private int tokenVersion = 0;

    public enum AuthType { LOCAL, KAKAO }

    public static User createLocal(String email, String encodedPassword, String nickname) {
        return new User(null, email, encodedPassword, null, nickname, AuthType.LOCAL, true, 0);
    }
    public static User createKakao(String kakaoId, String emailOrNull, String nickname) {
        return new User(null, emailOrNull, null, kakaoId, nickname, AuthType.KAKAO, true, 0);
    }

    public void changeNickname(String newNickname) { this.nickname = newNickname; }
    public void disable() { this.enabled = false; }
    public void bumpTokenVersion() { this.tokenVersion += 1; } // 글로벌 로그아웃
}
