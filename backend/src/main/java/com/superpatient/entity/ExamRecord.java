package com.superpatient.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExamRecord {
    private Long id;
    private Long userId;
    private Integer categoryId;
    private LocalDateTime examDate;
    private String hospital;
    private String doctor;
    private String notes;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
