package com.klei.product.mapper;

import com.klei.common.annotation.*;
import com.klei.product.entity.Follow;
import java.util.List;

public interface FollowMapper {

    @Select("SELECT * FROM follow WHERE user_id = ? AND follow_user_id = ? AND is_deleted = 0")
    Follow findByUserIdAndFollowUserId(Long userId, Long followUserId);

    @Select("SELECT * FROM follow WHERE user_id = ? AND is_deleted = 0 ORDER BY created_at DESC")
    List<Follow> findByUserId(Long userId);

    @Select("SELECT * FROM follow WHERE follow_user_id = ? AND is_deleted = 0 ORDER BY created_at DESC")
    List<Follow> findByFollowUserId(Long followUserId);

    @Insert("INSERT INTO follow (user_id, follow_user_id, is_deleted, created_at, updated_at) VALUES (?, ?, 0, NOW(), NOW())")
    int insert(Long userId, Long followUserId);

    @Update("UPDATE follow SET is_deleted = 1, updated_at = NOW() WHERE user_id = ? AND follow_user_id = ?")
    int deleteByUserIdAndFollowUserId(Long userId, Long followUserId);
}