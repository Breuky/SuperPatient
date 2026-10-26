package com.superpatient.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户全部记录的宏观统计，一次查完交给模型，
 * 避免它为了回答「一共几次」「哪项异常最多」而逐条翻记录。
 */
@Data
public class OverviewVO {

    private int totalRecords;
    private LocalDate firstDate;
    private LocalDate lastDate;
    /** 有数值的指标条数 */
    private int totalValues;
    /** 异常条数 */
    private int abnormalValues;

    private List<CategoryCount> categories = new ArrayList<>();
    private List<IndicatorCount> topAbnormal = new ArrayList<>();

    @Data
    public static class CategoryCount {
        private String name;
        private int count;
    }

    @Data
    public static class IndicatorCount {
        private String name;
        private String unit;
        private int times;
    }
}
