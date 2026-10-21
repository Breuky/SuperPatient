package com.superpatient.dto;

import lombok.Data;
import java.util.List;

@Data
public class RecordDetailVO {
    private Long id;
    private Long userId;
    private Integer categoryId;
    private String categoryName;
    private String examDate;
    private String hospital;
    private String doctor;
    private String notes;
    private String imageUrl;
    private List<ValueVO> values;
}
