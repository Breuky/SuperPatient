package com.superpatient.dto;

import lombok.Data;
import java.util.List;

@Data
public class CategoryVO {
    private Integer id;
    private String name;
    private String icon;
    private Integer sortOrder;
    private List<IndicatorVO> indicators;
}
