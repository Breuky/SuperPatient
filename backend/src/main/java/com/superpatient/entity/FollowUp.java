package com.superpatient.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 随访提醒：只由用户自身的历史间隔推导，不包含医疗建议 */
@Data
public class FollowUp {
    private Long id;
    private Long userId;
    private Integer indicatorId;
    private LocalDate lastExamDate;
    private Integer intervalDays;
    private LocalDate dueDate;
    private Integer priority;
    private String reason;
    private String note;
    /** pending / done / dismissed */
    private String status;
    private LocalDateTime createdAt;
}
