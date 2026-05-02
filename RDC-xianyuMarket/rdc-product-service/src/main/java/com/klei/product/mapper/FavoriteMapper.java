package com.klei.product.mapper;

import com.klei.common.annotation.*;
import com.klei.product.entity.Favorite;
import java.util.List;

public interface FavoriteMapper {

    @Select("SELECT * FROM favorite WHERE user_id = ? AND product_id = ? AND is_deleted = 0")
    Favorite findByUserIdAndProductId(Long userId, Long productId);

    @Select("SELECT * FROM favorite WHERE user_id = ? AND is_deleted = 0 ORDER BY created_at DESC")
    List<Favorite> findByUserId(Long userId);

    @Insert("INSERT INTO favorite (user_id, product_id, is_deleted, created_at, updated_at) VALUES (?, ?, 0, NOW(), NOW())")
    int insert(Long userId, Long productId);

    @Update("UPDATE favorite SET is_deleted = 1, updated_at = NOW() WHERE user_id = ? AND product_id = ?")
    int deleteByUserIdAndProductId(Long userId, Long productId);
}