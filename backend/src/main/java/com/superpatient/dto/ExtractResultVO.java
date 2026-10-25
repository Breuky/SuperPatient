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
        /** 精读模式下两次读数不一致时填充 */
        private Conflict conflict;
    }

    /**
     * 双读冲突：两次识别的同一行读数不同。
     * resolved 为仲裁结果，仲裁失败（看不清）时为 null，交由用户判断。
     */
    @Data
    public static class Conflict {
        private BigDecimal first;
        private BigDecimal second;
        private BigDecimal resolved;
        /** 仲裁给出的理由，例如「参考区间 316-354，34 缺一位」 */
        private String reason;
        /** 两次完全一致时为 true */
        private boolean agreement;
        /** 其中一次没有识别到这一行 */
        private boolean missingInOneRead;
    }

    @Data
    public static class Unmatched {
        private String rawName;
        private String rawValue;
        private String rawUnit;
        private String refText;
    }
}
