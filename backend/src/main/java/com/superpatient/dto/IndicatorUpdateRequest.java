package com.superpatient.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class IndicatorUpdateRequest {
    private Integer categoryId;
    private String name;
    /** 逗号分隔的别名；传空字符串表示清空 */
    private String aliases;
    private String unit;
    private String valueType;
    private BigDecimal normalMin;
    private BigDecimal normalMax;
    private Integer sortOrder;
}
