package com.klei.order.mapper;

import com.klei.common.annotation.*;
import com.klei.order.entity.WalletRecord;
import com.klei.order.entity.enums.WalletRecordType;
import java.math.BigDecimal;
import java.util.List;

public interface WalletRecordMapper {

    @Select("SELECT * FROM wallet_record WHERE id = ? AND is_deleted = 0")
    WalletRecord findById(Long id);

    @Select("SELECT * FROM wallet_record WHERE user_id = ? AND is_deleted = 0 ORDER BY created_at DESC")
    List<WalletRecord> findByUserId(Long userId);

    @Insert("INSERT INTO wallet_record (user_id, type, amount, balance_after, remark, is_deleted, created_at) VALUES (?, ?, ?, ?, ?, 0, NOW())")
    long insert(Long userId, WalletRecordType type, BigDecimal amount, BigDecimal balanceAfter, String remark);
}