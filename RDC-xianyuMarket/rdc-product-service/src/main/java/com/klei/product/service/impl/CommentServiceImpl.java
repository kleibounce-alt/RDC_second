package com.klei.product.service.impl;

import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.BusinessException;
import com.klei.product.entity.Comment;
import com.klei.product.entity.CommentLikes;
import com.klei.product.mapper.CommentLikesMapper;
import com.klei.product.mapper.CommentMapper;
import com.klei.product.service.CommentService;
import com.klei.product.vo.CommentVO;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private CommentLikesMapper commentLikesMapper;

    @Override
    public long addComment(Long productId, Long userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("评论内容不能为空");
        }
        return commentMapper.insert(productId, null, userId, content.trim());
    }

    @Override
    public long reply(Long productId, Long parentId, Long userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("回复内容不能为空");
        }
        if (parentId == null) {
            throw new BusinessException("回复目标不能为空");
        }
        Comment parent = commentMapper.findById(parentId);
        if (parent == null || parent.getIsDeleted() == 1) {
            throw new BusinessException("目标评论不存在");
        }
        return commentMapper.insert(productId, parentId, userId, content.trim());
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null || comment.getIsDeleted() == 1) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("无权删除他人评论");
        }
        commentMapper.deleteById(commentId);
    }

    @Override
    @Transactional
    public void toggleLike(Long commentId, Long userId) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null || comment.getIsDeleted() == 1) {
            throw new BusinessException("评论不存在");
        }

        CommentLikes like = commentLikesMapper.findByUserIdAndCommentId(userId, commentId);
        if (like == null || like.getIsDeleted() == 1) {
            commentLikesMapper.insert(userId, commentId);
            commentMapper.incrementLikeCount(commentId);
        } else {
            commentLikesMapper.deleteByUserIdAndCommentId(userId, commentId);
            commentMapper.decrementLikeCount(commentId);
        }
    }

    @Override
    public List<Comment> findByProductId(Long productId) {
        return commentMapper.findByProductId(productId);
    }

    @Override
    public List<CommentVO> findTreeByProductId(Long productId) {
        List<Comment> all = commentMapper.findByProductId(productId);
        List<CommentVO> top = all.stream()
                .filter(c -> c.getParentId() == null)
                .map(this::toVO)
                .collect(Collectors.toList());

        for (CommentVO vo : top) {
            vo.setChildren(all.stream()
                    .filter(c -> vo.getId().equals(c.getParentId()))
                    .map(this::toVO)
                    .collect(Collectors.toList()));
        }
        return top;
    }

    private CommentVO toVO(Comment c) {
        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setProductId(c.getProductId());
        vo.setParentId(c.getParentId());
        vo.setUserId(c.getUserId());
        vo.setContent(c.getContent());
        vo.setLikeCount(c.getLikeCount());
        vo.setCreatedAt(c.getCreatedAt());
        return vo;
    }
}