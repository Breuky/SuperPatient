package com.superpatient.dto;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String phone;
    /** 0=女 1=男 */
    private Integer gender;
    private String genderText;
    private String birthDate;
    private Integer age;
    private String notes;
}
