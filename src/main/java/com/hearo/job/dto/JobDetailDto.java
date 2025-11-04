package com.hearo.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDetailDto {
    private String rno;
    private String rnum;

    private String jobNm;
    private String busplaName;
    private String compAddr;
    private String cntctNo;
    private String empType;
    private String enterType;
    private String termDate;
    private String salary;
    private String salaryType;
    private String reqCareer;
    private String reqEduc;
    private String regagnName;
    private String offerregDt;
    private String regDt;

    // 작업환경 세부
    private String envBothHands;
    private String envEyesight;
    private String envHandwork;
    private String envLiftPower;
    private String envLstnTalk;
    private String envStndWalk;
}
