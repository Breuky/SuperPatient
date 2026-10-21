package com.superpatient.dto;

import lombok.Data;

@Data
public class TrendableIndicatorVO {
    private Integer indicatorId;
    private String indicatorName;
    private String unit;
    private Integer categoryId;
    private String categoryName;
    private Integer recordCount;
}
