package com.hearo.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPageDto {
    private int pageNo;          // 현재 페이지
    private int numOfRows;       // 페이지 크기
    private int totalCount;      // 총 개수 (필터 후 총량)
    private List<JobItemDto> items; // 데이터 목록
}
