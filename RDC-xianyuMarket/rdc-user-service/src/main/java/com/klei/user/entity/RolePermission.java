package com.klei.user.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RolePermission {
    private Long roleId;
    private Long permissionId;
    private Integer isDeleted;
    private LocalDateTime createdAt;
}