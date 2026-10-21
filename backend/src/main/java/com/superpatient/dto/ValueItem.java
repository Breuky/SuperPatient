package com.superpatient.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ValueItem {
    private Integer indicatorId;
    private BigDecimal valueDecimal;
    private String valueText;
    /** 本次报告的参考区间（可选，缺省时回落指标默认值） */
    private BigDecimal refMin;
    private BigDecimal refMax;
    private String refText;
    private String rawName;
    private String rawUnit;
}
