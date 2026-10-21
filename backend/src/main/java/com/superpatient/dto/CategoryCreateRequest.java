package com.superpatient.dto;

import lombok.Data;

@Data
public class CategoryCreateRequest {
    private String name;
    private String icon;
    private Integer sortOrder;
}
