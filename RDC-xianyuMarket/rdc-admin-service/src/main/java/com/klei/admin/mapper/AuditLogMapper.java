package com.klei.admin.mapper;

import com.klei.common.annotation.*;
import com.klei.admin.entity.AuditLog;
import com.klei.admin.entity.enums.AuditAction;
import java.util.List;

public interface AuditLogMapper {

    @Select("SELECT * FROM audit_log WHERE id = ? AND is_deleted = 0")
    AuditLog findById(Long id);

    @Select("SELECT * FROM audit_log WHERE product_id = ? AND is_deleted = 0 ORDER BY created_at DESC")
    List<AuditLog> findByProductId(Long productId);

    @Select("SELECT * FROM audit_log WHERE admin_id = ? AND is_deleted = 0 ORDER BY created_at DESC")
    List<AuditLog> findByAdminId(Long adminId);

    @Insert("INSERT INTO audit_log (product_id, admin_id, action, reason, is_deleted, created_at) VALUES (?, ?, ?, ?, 0, NOW())")
    long insert(Long productId, Long adminId, AuditAction action, String reason);

    @Update("UPDATE audit_log SET is_deleted = 1 WHERE id = ?")
    int deleteById(Long id);
}