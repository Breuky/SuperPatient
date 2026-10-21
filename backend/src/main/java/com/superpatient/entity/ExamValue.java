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
    private Integer isAbnormal;
    private LocalDateTime createdAt;
    // 非数据库字段，趋势查询时通过 JOIN 填充
    private LocalDateTime examDate;
}
