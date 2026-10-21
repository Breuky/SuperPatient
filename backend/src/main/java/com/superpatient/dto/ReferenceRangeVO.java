package com.superpatient.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReferenceRangeVO {
    private Integer id;
    private Integer indicatorId;
    /** null=不限 0=女 1=男 */
    private Integer gender;
    private Integer ageMin;
    private Integer ageMax;
    private BigDecimal refMin;
    private BigDecimal refMax;
    private String refText;
    private String source;
    private Integer priority;
}
