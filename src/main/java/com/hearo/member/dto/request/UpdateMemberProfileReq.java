package com.hearo.member.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hearo.member.domain.Gender;
import com.hearo.member.domain.InterestKeyword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record UpdateMemberProfileReq(

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
        String nickname,

        Gender gender,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Past(message = "생일은 오늘보다 과거 날짜여야 합니다.")
        LocalDate birthday,

        // null이면 서비스에서 그대로 넘겨 주고, 빈 리스트면 관심 키워드 초기화하는 식으로 처리 가능
        List<InterestKeyword> interestKeywords
) {}
