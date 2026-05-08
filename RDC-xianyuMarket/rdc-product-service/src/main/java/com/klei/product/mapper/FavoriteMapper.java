package com.klei.product.mapper;

import com.klei.common.annotation.*;
import com.klei.product.entity.Favorite;
import java.util.List;

public interface FavoriteMapper {

    @Select("SELECT * FROM favorite WHERE user_id = ? AND product_id = ? AND is_deleted = 0")
    Favorite findByUserIdAndProductId(Long userId, Long productId);

    @Select("SELECT * FROM favorite WHERE user_id = ? AND is_deleted = 0 ORDER BY created_at DESC")
    List<Favorite> findByUserId(Long userId);

    @Select("SELECT * FROM favorite WHERE user_id = ? AND is_deleted = 0 ORDER BY created_at DESC LIMIT ?, ?")
    List<Favorite> findByUserIdPage(Long userId, int offset, int size);

    @Select("SELECT COUNT(*) FROM favorite WHERE user_id = ? AND is_deleted = 0")
    long countByUserId(Long userId);

    @Insert("INSERT INTO favorite (user_id, product_id, is_deleted, created_at, updated_at) VALUES (?, ?, 0, NOW(), NOW()) ON DUPLICATE KEY UPDATE is_deleted = 0, updated_at = NOW()")
    int insert(Long userId, Long productId);

    @Delete("DELETE FROM favorite WHERE user_id = ? AND product_id = ?")
    int deleteByUserIdAndProductId(Long userId, Long productId);

    @Select("SELECT product_id FROM favorite WHERE user_id = ? AND is_deleted = 0")
    List<Long> findProductIdsByUserId(Long userId);
}