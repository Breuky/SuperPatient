package com.superpatient.dto;

import lombok.Data;
import java.util.List;

@Data
public class PatientDetailVO {
    private Long id;
    private String name;
    private Integer gender;
    private String genderText;
    private String birthDate;
    private Integer age;
    private String diagnosisDate;
    private String phone;
    private String notes;
    private PatientStats stats;

    @Data
    public static class PatientStats {
        private Integer totalExams;
        private String lastExamDate;
        private List<CategoryStat> categoryStats;
    }

    @Data
    public static class CategoryStat {
        private String categoryName;
        private Integer count;
        private String lastDate;
    }
}
