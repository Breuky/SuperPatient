package com.superpatient.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String nickname;
    private String avatarUrl;
    private String phone;
    /** 0=女 1=男，影响参考区间匹配 */
    private Integer gender;
    /** yyyy-MM-dd */
    private String birthDate;
    private String notes;
}
