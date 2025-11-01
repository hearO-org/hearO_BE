package com.hearo.job.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobRawResponse {

    private Header header;
    private Body body;

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultMsg;
        private String resultCode;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        /** 구인 정보 목록(items) */
        private Items items;
        /** 한 페이지 결과 수 */
        private Integer numOfRows;
        /** 페이지 번호 */
        private Integer pageNo;
        /** 전체 결과 수 */
        private Integer totalCount;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        private List<Item> item;
    }

    /** 개별 구인 정보 항목 */
    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String termDate;     // 모집기간 (예: 2025-10-29~2025-11-05)
        private String busplaName;   // 사업장명
        private String cntctNo;      // 연락처
        private String compAddr;     // 사업장주소
        private String empType;      // 고용형태 (계약직/상용직 등)
        private String enterType;    // 입사형태 (무관/경력 등)
        private String envBothHands; // 작업환경_양손사용
        private String envEyesight;  // 작업환경_시력
        private String envHandwork;  // 작업환경_손작업
        private String envLiftPower; // 작업환경_드는힘
        private String envLstnTalk;  // 작업환경_듣고 말하기
        private String envStndWalk;  // 작업환경_서거나 걷기
        private String jobNm;        // 모집직종
        private String offerregDt;   // 구인신청일자 (YYYYMMDD)
        private String regDt;        // 등록일 (YYYYMMDD)
        private String regagnName;   // 담당기관
        private String reqCareer;    // 요구경력
        private String reqEduc;      // 요구학력
        private String rno;          // 순번(식별자)
        private String rnum;         // 순번(중복 필드)
        private String salary;       // 임금
        private String salaryType;   // 임금형태 (월급/시급/연봉)
    }
}
