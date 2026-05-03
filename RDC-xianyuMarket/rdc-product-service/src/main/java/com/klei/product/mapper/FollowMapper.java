package com.klei.product.mapper;

import com.klei.common.annotation.*;
import com.klei.product.entity.Follow;
import com.klei.product.vo.FollowVO;

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

    @Select("SELECT u.id as follow_user_id, u.nickname, u.avatar, f.created_at FROM follow f JOIN sys_user u ON f.follow_user_id = u.id WHERE f.user_id = ? AND f.is_deleted = 0 AND u.is_deleted = 0 ORDER BY f.created_at DESC")
    List<FollowVO> findFollowsWithUser(Long userId);

    @Select("SELECT u.id as follow_user_id, u.nickname, u.avatar, f.created_at FROM follow f JOIN sys_user u ON f.user_id = u.id WHERE f.follow_user_id = ? AND f.is_deleted = 0 AND u.is_deleted = 0 ORDER BY f.created_at DESC")
    List<FollowVO> findFansWithUser(Long userId);
}