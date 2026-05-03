package com.klei.user.mapper;

import com.klei.common.annotation.*;
import com.klei.user.entity.User;
import com.klei.user.entity.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;

public interface UserMapper {

    @Select("SELECT * FROM sys_user WHERE id = ? AND is_deleted = 0")
    User findById(Long id);

    @Select("SELECT * FROM sys_user WHERE username = ? AND is_deleted = 0")
    User findByUsername(String username);

    @Insert("INSERT INTO sys_user (username, password, nickname, avatar, email, phone, status, is_deleted, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, 0, NOW(), NOW())")
    long insert(String username, String password, String nickname, String avatar, String email, String phone, UserStatus status);

    @Update("UPDATE sys_user SET nickname = ?, avatar = ?, email = ?, phone = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int updateProfile(String nickname, String avatar, String email, String phone, Long id);

    @Update("UPDATE sys_user SET password = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int updatePassword(String password, Long id);

    @Update("UPDATE sys_user SET status = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int updateStatus(UserStatus status, Long id);

    @Update("UPDATE sys_user SET ban_end_time = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int updateBanEndTime(LocalDateTime banEndTime, Long id);

    @Update("UPDATE sys_user SET vip_level = ?, vip_expire_time = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int updateVip(Integer vipLevel, LocalDateTime vipExpireTime, Long id);

    @Update("UPDATE sys_user SET is_deleted = 1, updated_at = NOW() WHERE id = ?")
    int deleteById(Long id);

    @Select("SELECT * FROM sys_user WHERE email = ? AND is_deleted = 0")
    User findByEmail(String email);

    @Update("UPDATE sys_user SET email = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int updateEmail(String email, Long id);

    @Select("SELECT * FROM sys_user WHERE (username LIKE ? OR nickname LIKE ?) AND is_deleted = 0 ORDER BY created_at DESC")
    List<User> search(String pattern, String pattern2);
}