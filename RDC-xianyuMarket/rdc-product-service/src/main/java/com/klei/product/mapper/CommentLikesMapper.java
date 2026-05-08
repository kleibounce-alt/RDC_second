package com.klei.product.mapper;

import com.klei.common.annotation.*;
import com.klei.product.entity.CommentLikes;

public interface CommentLikesMapper {

    @Select("SELECT * FROM comment_likes WHERE user_id = ? AND comment_id = ? AND is_deleted = 0")
    CommentLikes findByUserIdAndCommentId(Long userId, Long commentId);

    @Insert("INSERT INTO comment_likes (user_id, comment_id, is_deleted, created_at) VALUES (?, ?, 0, NOW()) ON DUPLICATE KEY UPDATE is_deleted = 0")
    int insert(Long userId, Long commentId);

    @Delete("DELETE FROM comment_likes WHERE user_id = ? AND comment_id = ?")
    int deleteByUserIdAndCommentId(Long userId, Long commentId);
}