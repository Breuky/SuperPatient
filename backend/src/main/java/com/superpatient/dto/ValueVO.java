package com.superpatient.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ValueVO {
    private Long id;
    private Integer indicatorId;
    private String indicatorName;
    private String unit;
    private String valueType;
    private BigDecimal valueDecimal;
    private String valueText;
    private Integer isAbnormal;
    private String abnormalText;
    private BigDecimal normalMin;
    private BigDecimal normalMax;
}
