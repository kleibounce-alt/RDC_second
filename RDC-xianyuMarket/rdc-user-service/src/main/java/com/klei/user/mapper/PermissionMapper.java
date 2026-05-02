package com.klei.user.mapper;

import com.klei.common.annotation.*;
import com.klei.user.entity.Permission;
import java.util.List;

public interface PermissionMapper {

    @Select("SELECT * FROM sys_permission WHERE id = ? AND is_deleted = 0")
    Permission findById(Long id);

    @Select("SELECT * FROM sys_permission WHERE code = ? AND is_deleted = 0")
    Permission findByCode(String code);

    @Select("SELECT * FROM sys_permission WHERE is_deleted = 0")
    List<Permission> findAll();

    @Insert("INSERT INTO sys_permission (name, code, url_pattern, is_deleted, created_at) VALUES (?, ?, ?, 0, NOW())")
    long insert(String name, String code, String urlPattern);

    @Update("UPDATE sys_permission SET is_deleted = 1 WHERE id = ?")
    int deleteById(Long id);
}