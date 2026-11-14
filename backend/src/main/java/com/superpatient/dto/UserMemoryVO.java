package com.superpatient.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 长期记忆条目：只用于调整表达方式，不作为事实来源 */
@Data
public class UserMemoryVO {
    private Integer id;
    private String kind;
    private String memoryKey;
    private String value;
    private String source;
    private LocalDateTime expiresAt;
}
