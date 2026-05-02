package com.klei.admin.entity;

import com.klei.admin.entity.enums.AuditAction;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLog {
    private Long id;
    private Long productId;
    private Long adminId;
    private AuditAction action;
    private String reason;
    private Integer isDeleted;
    private LocalDateTime createdAt;
}