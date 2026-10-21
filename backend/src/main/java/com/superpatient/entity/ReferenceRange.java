package com.superpatient.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 参考区间规则：同一指标在不同性别/年龄段区间不同。
 */
@Data
public class ReferenceRange {
    private Integer id;
    private Integer indicatorId;
    /** null=不限, 0=女, 1=男 */
    private Integer gender;
    private Integer ageMin;
    private Integer ageMax;
    private BigDecimal refMin;
    private BigDecimal refMax;
    private String refText;
    private String source;
    private Integer priority;
    private LocalDateTime createdAt;
}
