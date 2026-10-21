package com.superpatient.dto;

import lombok.Data;

@Data
public class PatientUpdateRequest {
    private String name;
    private Integer gender;
    private String birthDate;
    private String diagnosisDate;
    private String phone;
    private String notes;
}
