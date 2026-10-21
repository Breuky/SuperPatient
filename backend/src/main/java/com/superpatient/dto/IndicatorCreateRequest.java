package com.superpatient.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class IndicatorCreateRequest {
    private Integer categoryId;
    private String name;
    private String aliases;
    private String unit;
    private String valueType;
    private BigDecimal normalMin;
    private BigDecimal normalMax;
    private Integer sortOrder;
}
