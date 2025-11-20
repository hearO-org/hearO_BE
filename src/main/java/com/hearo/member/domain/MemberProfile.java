package com.hearo.member.domain;

import com.hearo.global.entity.BaseTimeEntity;
import com.hearo.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "member_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MemberProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User 1 : 1 MemberProfile
     * - 한 계정당 하나의 온보딩 프로필만 허용
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 30)
    private String nickname;           // 서비스에서 사용할 표시용 닉네임

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;            // "MALE", "FEMALE"

    private LocalDate birthday;        // 생일 (yyyy-MM-dd)

    /**
     * 관심 키워드는 간단한 문자열 리스트로 관리
     * ex) ["수어", "음악", "전시", "AI"]
     */
    @ElementCollection(targetClass = InterestKeyword.class)
    @CollectionTable(
            name = "member_interest_keywords",
            joinColumns = @JoinColumn(name = "member_profile_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "keyword", length = 200)
    @Builder.Default
    private List<InterestKeyword> interestKeywords = new ArrayList<>();

    /* ===== 정적 팩토리 메서드 ===== */

    public static MemberProfile create(User user, String nickname, Gender gender,
                                       LocalDate birthday, List<InterestKeyword> interestKeywords) {
        return MemberProfile.builder()
                .user(user)
                .nickname(nickname)
                .gender(gender)
                .birthday(birthday)
                .interestKeywords(
                        interestKeywords != null ? new ArrayList<>(interestKeywords) : new ArrayList<>()
                )
                .build();
    }

    /* ===== 수정 메서드 (향후 프로필 수정 API용) ===== */

    public void update(String nickname, Gender gender, LocalDate birthday, List<InterestKeyword> keywords) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        this.gender = gender;
        this.birthday = birthday;
        this.interestKeywords.clear();
        if (keywords != null && !keywords.isEmpty()) {
            this.interestKeywords.addAll(keywords);
        }
    }
}
