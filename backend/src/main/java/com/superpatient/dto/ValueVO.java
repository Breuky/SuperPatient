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
    /** report=报告印刷 rule=规则推算 default=指标默认 */
    private String refSource;
    /** 本次报告的快照区间（可能为空，为空表示用指标默认值） */
    private BigDecimal refMin;
    private BigDecimal refMax;
    /** 报告上的原始名称，用于展示"报告写的是 WBC" */
    private String rawName;
    private String rawUnit;
}
