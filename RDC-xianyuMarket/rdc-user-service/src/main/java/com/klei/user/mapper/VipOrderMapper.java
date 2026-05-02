package com.klei.user.mapper;

import com.klei.common.annotation.*;
import com.klei.user.entity.VipOrder;
import com.klei.user.entity.enums.VipOrderStatus;
import java.math.BigDecimal;
import java.util.List;

public interface VipOrderMapper {

    @Select("SELECT * FROM vip_order WHERE id = ? AND is_deleted = 0")
    VipOrder findById(Long id);

    @Select("SELECT * FROM vip_order WHERE user_id = ? AND is_deleted = 0 ORDER BY created_at DESC")
    List<VipOrder> findByUserId(Long userId);

    @Insert("INSERT INTO vip_order (user_id, vip_level, duration_months, price, status, is_deleted, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 0, NOW(), NOW())")
    long insert(Long userId, Integer vipLevel, Integer durationMonths, BigDecimal price, VipOrderStatus status);

    @Update("UPDATE vip_order SET status = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int updateStatus(VipOrderStatus status, Long id);
}