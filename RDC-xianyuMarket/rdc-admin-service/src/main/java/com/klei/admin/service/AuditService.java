package com.klei.admin.service;

import com.klei.admin.entity.AuditLog;
import com.klei.admin.vo.AuditProductVO;
import java.util.List;

public interface AuditService {
    List<AuditProductVO> findPendingProducts();
    void approve(Long productId, Long adminId);
    void reject(Long productId, Long adminId, String reason);
    List<AuditLog> findLogsByProductId(Long productId);
}