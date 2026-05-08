package com.klei.message.mapper;

import com.klei.common.annotation.*;
import com.klei.message.entity.Message;
import com.klei.message.entity.enums.MessageType;
import java.util.List;

public interface MessageMapper {

    @Select("SELECT * FROM message WHERE id = ? AND is_deleted = 0")
    Message findById(Long id);

    @Select("SELECT * FROM message WHERE user_id = ? AND is_deleted = 0 ORDER BY created_at DESC")
    List<Message> findByUserId(Long userId);

    @Select("SELECT * FROM message WHERE user_id = ? AND is_deleted = 0 ORDER BY created_at DESC LIMIT ?, ?")
    List<Message> findByUserIdPage(Long userId, int offset, int size);

    @Select("SELECT COUNT(*) FROM message WHERE user_id = ? AND is_deleted = 0")
    long countByUserId(Long userId);

    @Select("SELECT * FROM message WHERE user_id = ? AND is_read = 0 AND is_deleted = 0 ORDER BY created_at DESC")
    List<Message> findUnreadByUserId(Long userId);

    @Insert("INSERT INTO message (user_id, type, content, is_read, is_deleted, created_at) VALUES (?, ?, ?, 0, 0, NOW())")
    long insert(Long userId, MessageType type, String content);

    @Update("UPDATE message SET is_read = 1 WHERE id = ? AND is_deleted = 0")
    int markRead(Long id);

    @Update("UPDATE message SET is_read = 1 WHERE user_id = ? AND is_read = 0 AND is_deleted = 0")
    int markAllRead(Long userId);

    @Update("UPDATE message SET is_deleted = 1 WHERE id = ?")
    int deleteById(Long id);
}