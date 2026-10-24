package com.superpatient.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExamIndicator {

    /** 系统公共指标的归属值，所有用户可见 */
    public static final long SYSTEM_OWNER = 0L;

    private Integer id;
    private Integer categoryId;
    /** 0=系统公共，其他=用户私有 */
    private Long ownerId;
    private String name;
    /** 别名，逗号分隔，用于抽取结果归一 */
    private String aliases;
    private String unit;
    private String valueType;
    private BigDecimal normalMin;
    private BigDecimal normalMax;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;

    /** 是否对指定用户可见：公共指标人人可见，私有指标仅 owner 可见 */
    public boolean isVisibleTo(Long userId) {
        if (ownerId == null || ownerId == SYSTEM_OWNER) return true;
        return ownerId.equals(userId);
    }
}
