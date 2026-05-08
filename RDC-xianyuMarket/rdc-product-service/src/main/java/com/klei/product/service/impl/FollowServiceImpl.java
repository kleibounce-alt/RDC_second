package com.klei.product.service.impl;

import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.BusinessException;
import com.klei.common.mq.MqSender;
import com.klei.common.vo.PageResult;
import com.klei.product.entity.Follow;
import com.klei.product.mapper.FollowMapper;
import com.klei.product.service.FollowService;
import com.klei.product.vo.FollowVO;

@Component
public class FollowServiceImpl implements FollowService {

    @Autowired
    private FollowMapper followMapper;

    @Override
    @Transactional
    public void toggleFollow(Long userId, Long followUserId) {
        if (userId.equals(followUserId)) {
            throw new BusinessException("不能关注自己");
        }

        Follow follow = followMapper.findByUserIdAndFollowUserId(userId, followUserId);
        if (follow == null || follow.getIsDeleted() == 1) {
            followMapper.insert(userId, followUserId);
            MqSender.sendMessage(followUserId, "SYSTEM", "您有一个新粉丝");
        } else {
            followMapper.deleteByUserIdAndFollowUserId(userId, followUserId);
        }
    }

    @Override
    public PageResult<FollowVO> findMyFollows(Long userId, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        int offset = (page - 1) * size;
        PageResult<FollowVO> result = new PageResult<>();
        result.setList(followMapper.findFollowsWithUserPage(userId, offset, size));
        result.setTotal(followMapper.countFollows(userId));
        result.setPage(page);
        result.setSize(size);
        return result;
    }

    @Override
    public PageResult<FollowVO> findMyFans(Long userId, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        int offset = (page - 1) * size;
        PageResult<FollowVO> result = new PageResult<>();
        result.setList(followMapper.findFansWithUserPage(userId, offset, size));
        result.setTotal(followMapper.countFans(userId));
        result.setPage(page);
        result.setSize(size);
        return result;
    }
}