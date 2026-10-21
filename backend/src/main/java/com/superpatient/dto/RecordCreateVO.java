package com.superpatient.dto;

import lombok.Data;
import java.util.List;

@Data
public class RecordCreateVO {
    private Long id;
    private Long patientId;
    private Integer categoryId;
    private String examDate;
    private Integer abnormalCount;
    private List<ValueBriefVO> values;
}
