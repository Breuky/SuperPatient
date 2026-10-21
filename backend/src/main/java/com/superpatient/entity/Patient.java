package com.superpatient.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Patient {
    private Long id;
    private Long userId;
    private String name;
    private Integer gender;
    private LocalDate birthDate;
    private LocalDate diagnosisDate;
    private String phone;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
