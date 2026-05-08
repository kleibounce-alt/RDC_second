package com.klei.user.mapper;

import com.klei.common.annotation.*;
import com.klei.user.entity.Role;
import java.util.List;

public interface UserRoleMapper {

    @Select("SELECT r.* FROM sys_role r JOIN sys_user_role ur ON r.id = ur.role_id WHERE ur.user_id = ? AND r.is_deleted = 0 AND ur.is_deleted = 0")
    List<Role> findRolesByUserId(Long userId);

    @Insert("INSERT INTO sys_user_role (user_id, role_id, is_deleted, created_at) VALUES (?, ?, 0, NOW())")
    int insert(Long userId, Long roleId);

    @Update("UPDATE sys_user_role SET is_deleted = 1 WHERE user_id = ?")
    int deleteByUserId(Long userId);

    @Update("UPDATE sys_user_role SET is_deleted = 1 WHERE user_id = ? AND role_id = ?")
    int deleteByUserIdAndRoleId(Long userId, Long roleId);

    @Select("SELECT ur.user_id FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id WHERE r.code = ? AND ur.is_deleted = 0 AND r.is_deleted = 0")
    List<Long> findUserIdsByRoleCode(String roleCode);
}