package com.klei.message.mapper;

import com.klei.common.annotation.*;
import com.klei.message.entity.SensitiveWord;
import java.util.List;

public interface SensitiveWordMapper {

    @Select("SELECT * FROM sensitive_word WHERE id = ? AND is_deleted = 0")
    SensitiveWord findById(Long id);

    @Select("SELECT * FROM sensitive_word WHERE word = ? AND is_deleted = 0")
    SensitiveWord findByWord(String word);

    @Select("SELECT * FROM sensitive_word WHERE is_deleted = 0")
    List<SensitiveWord> findAll();

    @Insert("INSERT INTO sensitive_word (word, is_deleted, created_at) VALUES (?, 0, NOW())")
    long insert(String word);

    @Update("UPDATE sensitive_word SET is_deleted = 1 WHERE id = ?")
    int deleteById(Long id);

    @Update("UPDATE sensitive_word SET is_deleted = 0 WHERE id = ?")
    int restoreById(Long id);
}