package com.klei.product.mapper;

import com.klei.common.annotation.*;
import com.klei.product.entity.Tag;
import java.util.List;

public interface TagMapper {

    @Select("SELECT * FROM tag WHERE id = ? AND is_deleted = 0")
    Tag findById(Long id);

    @Select("SELECT * FROM tag WHERE name = ? AND is_deleted = 0")
    Tag findByName(String name);

    @Select("SELECT * FROM tag WHERE is_deleted = 0")
    List<Tag> findAll();

    @Insert("INSERT INTO tag (name, is_deleted, created_at) VALUES (?, 0, NOW())")
    long insert(String name);

    @Update("UPDATE tag SET is_deleted = 1 WHERE id = ?")
    int deleteById(Long id);
}