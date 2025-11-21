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

        // 닉네임 변경 (null 또는 blank면 무시)
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }

        // 성별 변경 (null이면 기존 값 유지)
        if (gender != null) {
            this.gender = gender;
        }

        // 생일 변경 (null이면 기존 값 유지)
        if (birthday != null) {
            this.birthday = birthday;
        }

        /**
         * 관심 키워드 업데이트 정책
         * - keywords == null   → 아무 변경도 하지 않고 기존 값 유지
         * - keywords == []     → clear() 실행 → 관심 키워드 전체 삭제(초기화)
         * - keywords.size > 0  → 기존 clear() 후 전달한 리스트로 교체
         */
        if (keywords != null) { // null이면 기존 값 유지
            this.interestKeywords.clear();
            if (!keywords.isEmpty()) {
                this.interestKeywords.addAll(keywords);
            }
        }
    }

}
