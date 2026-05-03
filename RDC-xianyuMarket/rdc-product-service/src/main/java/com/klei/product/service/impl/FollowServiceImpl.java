package com.klei.product.service.impl;

import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.BusinessException;
import com.klei.product.entity.Follow;
import com.klei.product.mapper.FollowMapper;
import com.klei.product.service.FollowService;
import com.klei.product.vo.FollowVO;

import java.util.List;

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
        } else {
            followMapper.deleteByUserIdAndFollowUserId(userId, followUserId);
        }
    }

    @Override
    public List<FollowVO> findMyFollows(Long userId) {
        return followMapper.findFollowsWithUser(userId);
    }

    @Override
    public List<FollowVO> findMyFans(Long userId) {
        return followMapper.findFansWithUser(userId);
    }
}