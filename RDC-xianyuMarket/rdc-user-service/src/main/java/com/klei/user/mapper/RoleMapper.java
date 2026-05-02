package com.klei.user.mapper;

import com.klei.common.annotation.*;
import com.klei.user.entity.Role;
import java.util.List;

public interface RoleMapper {

    @Select("SELECT * FROM sys_role WHERE id = ? AND is_deleted = 0")
    Role findById(Long id);

    @Select("SELECT * FROM sys_role WHERE code = ? AND is_deleted = 0")
    Role findByCode(String code);

    @Select("SELECT * FROM sys_role WHERE is_deleted = 0")
    List<Role> findAll();

    @Insert("INSERT INTO sys_role (name, code, is_deleted, created_at) VALUES (?, ?, 0, NOW())")
    long insert(String name, String code);

    @Update("UPDATE sys_role SET is_deleted = 1 WHERE id = ?")
    int deleteById(Long id);
}