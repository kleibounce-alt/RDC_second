package com.klei.user.mapper;

import com.klei.common.annotation.*;
import com.klei.user.entity.Permission;
import java.util.List;

public interface RolePermissionMapper {

    @Select("SELECT p.* FROM sys_permission p JOIN sys_role_permission rp ON p.id = rp.permission_id WHERE rp.role_id = ? AND p.is_deleted = 0 AND rp.is_deleted = 0")
    List<Permission> findPermissionsByRoleId(Long roleId);

    @Insert("INSERT INTO sys_role_permission (role_id, permission_id, is_deleted, created_at) VALUES (?, ?, 0, NOW())")
    int insert(Long roleId, Long permissionId);

    @Update("UPDATE sys_role_permission SET is_deleted = 1 WHERE role_id = ?")
    int deleteByRoleId(Long roleId);
}