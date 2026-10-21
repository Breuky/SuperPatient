package com.superpatient.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TrendVO {
    private TrendIndicator indicator;
    private List<TrendPoint> points;

    @Data
    public static class TrendIndicator {
        private Integer id;
        private String name;
        private String unit;
        private BigDecimal normalMin;
        private BigDecimal normalMax;
    }

    @Data
    public static class TrendPoint {
        private Long recordId;
        private String examDate;
        private BigDecimal value;
        private Integer isAbnormal;
        /** 该次报告自己的参考区间，不同医院可能不同 */
        private BigDecimal refMin;
        private BigDecimal refMax;
        private String refText;
    }
}
