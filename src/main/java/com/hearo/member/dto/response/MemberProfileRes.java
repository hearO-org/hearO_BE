package com.hearo.member.dto.response;

import com.hearo.member.domain.Gender;
import com.hearo.member.domain.InterestKeyword;

import java.time.LocalDate;
import java.util.List;

public record MemberProfileRes(
        Long userId,
        String email,
        String nickname,
        Gender gender,
        LocalDate birthday,
        List<InterestKeyword> interestKeywords
) {}
