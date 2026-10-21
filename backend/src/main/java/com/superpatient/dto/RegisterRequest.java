package com.superpatient.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String nickname;
    /** 0=女 1=男，注册时必填（参考区间按性别匹配） */
    private Integer gender;
}
