package com.klei.product.mapper;

import com.klei.common.annotation.*;
import com.klei.product.entity.Comment;
import java.util.List;

public interface CommentMapper {

    @Select("SELECT * FROM comment WHERE id = ? AND is_deleted = 0")
    Comment findById(Long id);

    @Select("SELECT * FROM comment WHERE product_id = ? AND is_deleted = 0 ORDER BY created_at DESC")
    List<Comment> findByProductId(Long productId);

    @Insert("INSERT INTO comment (product_id, user_id, content, like_count, is_deleted, created_at) VALUES (?, ?, ?, 0, 0, NOW())")
    long insert(Long productId, Long userId, String content);

    @Update("UPDATE comment SET like_count = like_count + 1 WHERE id = ? AND is_deleted = 0")
    int incrementLikeCount(Long id);

    @Update("UPDATE comment SET is_deleted = 1 WHERE id = ?")
    int deleteById(Long id);
}