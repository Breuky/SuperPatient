package com.superpatient.dto;

import lombok.Data;

/** 按题型聚合的调用统计 */
@Data
public class InsightStatVO {
    private String category;
    private Integer count;
    private Double avgTurns;
    private Double avgTools;
    private Integer avgTokens;
    private Integer avgElapsedMs;
    /** 该题型里降级过几次 */
    private Integer degradedCount;
}
