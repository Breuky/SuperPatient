package com.superpatient.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 一次 Agent 调用的留痕，用于观察真实使用情况 */
@Data
public class InsightLog {
    private Long id;
    private Long userId;
    private Long recordId;
    private String mode;
    private String question;
    /** 题型：abnormal/trend/cross_record/value/summary/boundary/other */
    private String category;
    private Integer turns;
    private Integer toolCalls;
    private Integer tokens;
    private Integer elapsedMs;
    private Integer degraded;
    private String summary;
    private String traceJson;
    private LocalDateTime createdAt;
}
