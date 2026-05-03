package com.klei.user.service;

import com.klei.user.dto.VipOrderDTO;
import com.klei.user.vo.VipOrderVO;
import java.util.List;

public interface VipService {

    List<VipOrderVO> findByUserId(Long userId);

    long createOrder(Long userId, VipOrderDTO dto);

    void payOrder(Long orderId);
}