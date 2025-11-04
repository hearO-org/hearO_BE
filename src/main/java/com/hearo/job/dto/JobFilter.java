package com.hearo.job.dto;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class JobFilter {
    private String keyword;     // jobNm/busplaName/compAddr 포함 검색
    private String region;      // 주소 포함 매칭(공백 토큰 AND)
    private String empType;     // 고용형태
    private String salaryType;  // 임금형태
    private String enterType;   // 입사형태

    // 환경 조건(선택)
    private String envBothHands;
    private String envEyesight;
    private String envHandwork;
    private String envLiftPower;
    private String envLstnTalk;
    private String envStndWalk;

    public boolean hasAny() {
        return StringUtils.hasText(keyword) || StringUtils.hasText(region) ||
               StringUtils.hasText(empType) || StringUtils.hasText(salaryType) ||
               StringUtils.hasText(enterType) ||
               StringUtils.hasText(envBothHands) || StringUtils.hasText(envEyesight) ||
               StringUtils.hasText(envHandwork) || StringUtils.hasText(envLiftPower) ||
               StringUtils.hasText(envLstnTalk) || StringUtils.hasText(envStndWalk);
    }
}
