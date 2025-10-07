package com.hearo.signlanguage.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SignPageDto {
    private List<SignItemDto> items;
    private int pageNo;
    private int numOfRows;
    private int totalCount;
}
