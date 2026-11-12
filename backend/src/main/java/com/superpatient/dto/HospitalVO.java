package com.superpatient.dto;

import lombok.Data;

/** 导诊用的医院-科室条目。verified=false 表示资质/来源尚未人工核对，前端需明示 */
@Data
public class HospitalVO {
    private Integer hospitalId;
    private String name;
    private String city;
    private String district;
    private String level;
    /** 综合 / 中医 / 专科 */
    private String hospitalType;
    private String address;
    private String bookingUrl;
    private String department;
    /** 国家/省级临床重点专科，查不到为空 */
    private String specialtyLevel;
    private String sourceUrl;
    /** 医院等级是否已核对（有官方文件来源） */
    private boolean verified;
    /** 该院该科室的资质是否已核对（重点专科标注来自官网） */
    private boolean departmentVerified;
}