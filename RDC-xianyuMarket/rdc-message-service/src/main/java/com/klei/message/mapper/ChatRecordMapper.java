package com.klei.message.mapper;

import com.klei.common.annotation.*;
import com.klei.message.entity.ChatRecord;
import java.util.List;

public interface ChatRecordMapper {

    @Select("SELECT * FROM chat_record WHERE id = ? AND is_deleted = 0")
    ChatRecord findById(Long id);

    @Select("SELECT * FROM chat_record WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) AND is_deleted = 0 ORDER BY created_at ASC")
    List<ChatRecord> findDialog(Long userA, Long userB, Long userB2, Long userA2);

    @Insert("INSERT INTO chat_record (sender_id, receiver_id, product_id, content, is_deleted, created_at) VALUES (?, ?, ?, ?, 0, NOW())")
    long insert(Long senderId, Long receiverId, Long productId, String content);

    @Update("UPDATE chat_record SET is_deleted = 1 WHERE id = ?")
    int deleteById(Long id);
}