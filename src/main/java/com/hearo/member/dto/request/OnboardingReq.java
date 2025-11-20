package com.hearo.member.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hearo.member.domain.Gender;
import com.hearo.member.domain.InterestKeyword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record OnboardingReq(

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
        String nickname,

        // "MALE"/"FEMALE"
        Gender gender,

        // JSON Body로 받을 때 사용 (예: "2000-01-01")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Past(message = "생일은 오늘보다 과거 날짜여야 합니다.")
        LocalDate birthday,

        // 관심 키워드: null 허용, 비어 있으면 빈 리스트 처리
        List<InterestKeyword> interestKeywords
) {}
