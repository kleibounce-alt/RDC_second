package com.klei.product.service;

import com.klei.product.entity.Comment;
import com.klei.product.vo.CommentVO;
import java.util.List;

public interface CommentService {

    long addComment(Long productId, Long userId, String content);

    long reply(Long productId, Long parentId, Long userId, String content);

    void deleteComment(Long commentId, Long userId);

    void toggleLike(Long commentId, Long userId);

    List<Comment> findByProductId(Long productId);

    List<CommentVO> findTreeByProductId(Long productId);
}