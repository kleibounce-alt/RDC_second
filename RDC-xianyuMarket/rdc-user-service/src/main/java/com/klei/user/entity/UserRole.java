package com.klei.user.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserRole {
    private Long userId;
    private Long roleId;
    private Integer isDeleted;
    private LocalDateTime createdAt;
}