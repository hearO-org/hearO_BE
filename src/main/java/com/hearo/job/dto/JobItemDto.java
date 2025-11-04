package com.hearo.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobItemDto {
    private String rno;          // 순번 (고유 식별자)
    private String jobNm;        // 모집직종명
    private String busplaName;   // 사업장명
    private String compAddr;     // 사업장주소
    private String cntctNo;      // 연락처
    private String empType;      // 고용형태
    private String enterType;    // 입사형태
    private String salary;       // 임금 금액(문자열)
    private String salaryType;   // 임금 형태
    private String termDate;     // 모집기간
    private String offerregDt;   // 구인신청일자
    private String regDt;        // 등록일
    private String regagnName;   // 담당기관
    private String reqCareer;    // 요구경력
    private String reqEduc;      // 요구학력
}
