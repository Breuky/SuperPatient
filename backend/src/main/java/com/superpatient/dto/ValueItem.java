package com.superpatient.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ValueItem {
    private Integer indicatorId;
    private BigDecimal valueDecimal;
    private String valueText;
}
