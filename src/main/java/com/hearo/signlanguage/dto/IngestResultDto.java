package com.hearo.signlanguage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IngestResultDto {
    private int totalCount;
    private int totalFetched;
    private int inserted;
    private int updated;
    private int pageSize;
}
