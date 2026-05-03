package com.klei.admin.service.impl;

import com.klei.admin.service.AdminUserService;
import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.BusinessException;
import com.klei.common.utils.RedisUtil;
import com.klei.product.entity.Comment;
import com.klei.product.entity.Product;
import com.klei.product.entity.enums.ProductStatus;
import com.klei.product.mapper.CommentMapper;
import com.klei.product.mapper.ProductMapper;
import com.klei.user.entity.User;
import com.klei.user.entity.enums.UserStatus;
import com.klei.user.mapper.UserMapper;

import java.time.LocalDateTime;

@Component
public class AdminUserServiceImpl implements AdminUserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CommentMapper commentMapper;

    @Override
    @Transactional
    public void banUser(Long userId, LocalDateTime banEndTime) {
        if (banEndTime == null || banEndTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("封禁时间必须大于当前时间");
        }
        User user = userMapper.findById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }
        userMapper.updateBanEndTime(banEndTime, userId);
        userMapper.updateStatus(UserStatus.BANNED, userId);
    }

    @Override
    @Transactional
    public void unbanUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }
        userMapper.updateBanEndTime(null, userId);
        userMapper.updateStatus(UserStatus.NORMAL, userId);
    }

    @Override
    @Transactional
    public void forceOffShelf(Long productId) {
        Product product = productMapper.findById(productId);
        if (product == null || product.getIsDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }
        int rows = productMapper.forceOffShelf("管理员强制下架", productId);
        if (rows == 0) {
            throw new BusinessException("商品状态不允许下架或已被处理");
        }
        RedisUtil.del("product:detail:" + productId);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null || comment.getIsDeleted() == 1) {
            throw new BusinessException("评论不存在");
        }
        commentMapper.deleteById(commentId);
    }
}