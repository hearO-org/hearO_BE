package com.hearo.member.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum InterestKeyword {

    EMPLOYMENT("취업"),                  // 취업 정보, 직무, 이력서, 면접, 취업 프로그램
    WELFARE("복지"),                    // 복지 제도, 수당, 지원금, 활동보조 등
    EDUCATION("교육"),                  // 수업, 강의, 평생교육, 대학/학원 등
    COUNSELING("상담"),                 // 심리상담, 진로상담, 권익 옹호
    ACCESSIBILITY_TECH("보조공학·기술"), // 보청기, 자막앱, AI보조도구 등
    SUBTITLE_MEDIA("자막·영상콘텐츠"),   // 자막 지원 OTT, 유튜브, 영상
    CULTURE_MUSICAL("뮤지컬"),          // 뮤지컬 관람
    CULTURE_CONCERT("공연·콘서트"),      // 공연, 콘서트, 페스티벌
    CULTURE_MOVIE("영화·상영회"),       // 영화관, 시사회, 상영회
    COMMUNITY_MEETUP("모임·커뮤니티"),   // 청각장애인 모임, 또래 커뮤니티, 동아리
    DAILY_LIFE("생활정보"),             // 교통, 병원, 관공서, 민원 등 일상생활
    POLICY_RIGHTS("정책·권익"),         // 장애인 권리, 제도, 법/정책
    SPORTS("스포츠·운동"),              // 수영, 헬스, 요가 등
    HOBBY("취미·여가");                 // 게임, 독서, 공예 등

    private final String label;

    InterestKeyword(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        // 응답 JSON에 "취업", "복지"처럼 한글 라벨로 나가게 할 수도 있음
        return label;
    }

    @JsonCreator
    public static InterestKeyword from(String value) {
        // "EMPLOYMENT" 또는 "취업" 둘 다 허용하고 싶으면 이렇게 처리
        for (InterestKeyword keyword : values()) {
            if (keyword.name().equalsIgnoreCase(value) || keyword.label.equals(value)) {
                return keyword;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 관심 키워드: " + value);
    }
}
