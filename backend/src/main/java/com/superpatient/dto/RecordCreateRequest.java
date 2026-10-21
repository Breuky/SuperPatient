package com.superpatient.dto;

import lombok.Data;
import java.util.List;

@Data
public class RecordCreateRequest {
    private Integer categoryId;
    private String examDate;
    private String hospital;
    private String doctor;
    private String notes;
    private String imageUrl;
    private List<ValueItem> values;
}
