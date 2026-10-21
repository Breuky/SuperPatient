package com.superpatient.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExamCategory {
    private Integer id;
    private String name;
    private String icon;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
