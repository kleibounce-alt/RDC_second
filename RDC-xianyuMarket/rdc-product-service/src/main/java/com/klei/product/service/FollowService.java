package com.klei.product.service;

import com.klei.common.vo.PageResult;
import com.klei.product.vo.FollowVO;

public interface FollowService {

    void toggleFollow(Long userId, Long followUserId);

    PageResult<FollowVO> findMyFollows(Long userId, int page, int size);

    PageResult<FollowVO> findMyFans(Long userId, int page, int size);
}