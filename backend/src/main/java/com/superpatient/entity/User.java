package com.superpatient.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    /** BCrypt 哈希，绝不出现在任何响应里 */
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
