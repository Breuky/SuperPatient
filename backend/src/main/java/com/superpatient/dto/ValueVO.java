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
    /** 生效的参考区间：优先取本次报告值，其次指标默认值 */
    private BigDecimal normalMin;
    private BigDecimal normalMax;
    private String refText;
    /** 报告上的原始名称，用于展示"报告写的是 WBC" */
    private String rawName;
}
