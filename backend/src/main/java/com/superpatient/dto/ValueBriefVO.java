package com.superpatient.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ValueBriefVO {
    private Integer indicatorId;
    private String indicatorName;
    private BigDecimal valueDecimal;
    private Integer isAbnormal;
    private String abnormalText;
}
