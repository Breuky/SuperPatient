package com.superpatient.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExamValue {
    private Long id;
    private Long recordId;
    private Integer indicatorId;
    private BigDecimal valueDecimal;
    private String valueText;
    /** 本次报告印刷的参考区间（快照，优先于指标默认值） */
    private BigDecimal refMin;
    private BigDecimal refMax;
    private String refText;
    /** 区间来源: report=报告印刷 rule=规则推算, null=未存储(读取时回落) */
    private String refSource;
    /** 模型抽取得到的原文，用于回溯与准确率评测 */
    private String rawName;
    private String rawUnit;
    private Integer isAbnormal;
    private LocalDateTime createdAt;
    // 非数据库字段，趋势查询时通过 JOIN 填充
    private LocalDateTime examDate;
}
