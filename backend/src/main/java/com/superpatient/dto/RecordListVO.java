package com.superpatient.dto;

import lombok.Data;

@Data
public class RecordListVO {
    private Long id;
    private Integer categoryId;
    private String categoryName;
    private String examDate;
    private String hospital;
    private String doctor;
    private Integer abnormalCount;
    private Integer totalIndicators;
    private String imageUrl;
}
