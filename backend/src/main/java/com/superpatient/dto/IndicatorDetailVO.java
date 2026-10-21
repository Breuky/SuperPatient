package com.superpatient.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class IndicatorDetailVO {
    private Integer id;
    private Integer categoryId;
    private String name;
    /** 逗号分隔的别名，用于抽取归一 */
    private String aliases;
    private String unit;
    private String valueType;
    private BigDecimal normalMin;
    private BigDecimal normalMax;
    private Integer sortOrder;
    private Integer status;
}
