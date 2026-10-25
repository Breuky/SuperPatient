package com.superpatient.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 某个指标最近一次的值，用于抽取时的基线比对 */
@Data
public class IndicatorLastValue {
    private Integer indicatorId;
    private BigDecimal valueDecimal;
}
