package com.superpatient.dto;

import lombok.Data;

@Data
public class PatientListVO {
    private Long id;
    private String name;
    private Integer gender;
    private String genderText;
    private Integer age;
    private String diagnosisDate;
    private String lastExamDate;
    private Integer totalExams;
    private Integer abnormalCount;
}
