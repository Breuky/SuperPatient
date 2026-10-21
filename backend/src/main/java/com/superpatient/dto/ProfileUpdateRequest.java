package com.superpatient.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String nickname;
    private String avatarUrl;
    private String phone;
}
