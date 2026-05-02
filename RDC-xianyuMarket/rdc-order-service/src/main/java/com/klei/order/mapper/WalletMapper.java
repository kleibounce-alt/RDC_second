package com.klei.order.mapper;

import com.klei.common.annotation.*;
import com.klei.order.entity.Wallet;
import java.math.BigDecimal;

public interface WalletMapper {

    @Select("SELECT * FROM wallet WHERE id = ? AND is_deleted = 0")
    Wallet findById(Long id);

    @Select("SELECT * FROM wallet WHERE user_id = ? AND is_deleted = 0")
    Wallet findByUserId(Long userId);

    @Insert("INSERT INTO wallet (user_id, balance, is_deleted, created_at, updated_at) VALUES (?, 0.00, 0, NOW(), NOW())")
    long insert(Long userId);

    @Update("UPDATE wallet SET balance = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int updateBalance(BigDecimal balance, Long id);

    @Update("UPDATE wallet SET is_deleted = 1, updated_at = NOW() WHERE id = ?")
    int deleteById(Long id);
}