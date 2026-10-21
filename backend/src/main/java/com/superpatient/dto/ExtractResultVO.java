package com.superpatient.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 模型抽取结果。只返回候选值，不落库，由用户在校对页确认后再提交。
 */
@Data
public class ExtractResultVO {

    private Integer categoryId;
    private String examDate;
    private String hospital;
    /** 实际使用的模型标识，便于对照实验 */
    private String engine;
    private List<Candidate> candidates;
    private List<Unmatched> unmatched;

    @Data
    public static class Candidate {
        private String rawName;
        private Integer indicatorId;
        private String indicatorName;
        /** 归一后单位 */
        private String unit;
        private String rawUnit;
        private BigDecimal valueDecimal;
        private String valueText;
        private BigDecimal refMin;
        private BigDecimal refMax;
        private String refText;
        private Integer isAbnormal;
        /** exact / alias / contains */
        private String matchType;
        private Double confidence;
        /** 单位换算、数值异常等提示，供校对页高亮 */
        private String warning;
    }

    @Data
    public static class Unmatched {
        private String rawName;
        private String rawValue;
        private String rawUnit;
        private String refText;
    }
}
