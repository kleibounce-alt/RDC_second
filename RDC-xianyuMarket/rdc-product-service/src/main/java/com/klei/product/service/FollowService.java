package com.klei.product.service;

import com.klei.product.vo.FollowVO;
import java.util.List;

public interface FollowService {

    void toggleFollow(Long userId, Long followUserId);

    List<FollowVO> findMyFollows(Long userId);

    List<FollowVO> findMyFans(Long userId);
}