package com.superpatient.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户长期关注的指标。用于录入页与趋势页优先展示。
 */
@Data
public class UserIndicatorFocus {
    private Long id;
    private Long userId;
    private Integer indicatorId;
    private LocalDateTime createdAt;
}
