package com.superpatient.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FollowUpVO {
    private Long id;
    private Integer indicatorId;
    private String indicatorName;
    private String unit;
    private LocalDate lastExamDate;
    /** 该指标既往复查间隔中位数 */
    private Integer intervalDays;
    private LocalDate dueDate;
    /** 已过期天数，未到期为 0 */
    private Long overdueDays;
    private Integer priority;
    /** 规则生成的依据，只陈述事实 */
    private String reason;
    /** Agent 润色后的一句话；为空时前端用 reason */
    private String note;
    private String status;
}
