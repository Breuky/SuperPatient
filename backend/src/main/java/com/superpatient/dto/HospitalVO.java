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
    private String address;
    private String bookingUrl;
    private String department;
    /** 国家/省级临床重点专科，查不到为空 */
    private String specialtyLevel;
    private String sourceUrl;
    /** 是否已人工核对（source_url 与 verified_at 都非空） */
    private boolean verified;
}
