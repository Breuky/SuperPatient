package com.superpatient.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class IndicatorDetailVO {
    private Integer id;
    private Integer categoryId;
    /** 0=系统公共，其他=创建者的用户ID */
    private Long ownerId;
    private String name;
    /** 逗号分隔的别名，用于抽取归一 */
    private String aliases;
    private String unit;
    private String valueType;
    private BigDecimal normalMin;
    private BigDecimal normalMax;
    private Integer sortOrder;
    private Integer status;
}
