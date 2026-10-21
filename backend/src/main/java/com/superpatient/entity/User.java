package com.superpatient.entity;

import lombok.Data;
import java.time.LocalDate;
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
    /** 0=女 1=男，参考区间匹配用 */
    private Integer gender;
    private LocalDate birthDate;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
