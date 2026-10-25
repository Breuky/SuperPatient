package com.superpatient.dto;

import lombok.Data;

import java.time.LocalDate;

/** 计算提醒用的原始历史行 */
@Data
public class FollowUpSource {
    private Integer indicatorId;
    private String indicatorName;
    private String unit;
    private LocalDate examDate;
    private Integer isAbnormal;
}
