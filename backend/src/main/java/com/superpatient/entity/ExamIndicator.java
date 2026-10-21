package com.superpatient.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExamIndicator {
    private Integer id;
    private Integer categoryId;
    private String name;
    /** 别名，逗号分隔，用于抽取结果归一 */
    private String aliases;
    private String unit;
    private String valueType;
    private BigDecimal normalMin;
    private BigDecimal normalMax;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
