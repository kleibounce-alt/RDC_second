package com.klei.user.service;

import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.BusinessException;
import com.klei.user.dto.VipOrderDTO;
import com.klei.user.entity.VipOrder;
import com.klei.user.entity.enums.VipOrderStatus;
import com.klei.user.mapper.UserMapper;
import com.klei.user.mapper.VipOrderMapper;
import com.klei.user.vo.VipOrderVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class VipService {

    @Autowired
    private VipOrderMapper vipOrderMapper;
    @Autowired
    private UserMapper userMapper;

    public List<VipOrderVO> findByUserId(Long userId) {
        return vipOrderMapper.findByUserId(userId).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Transactional
    public long createOrder(Long userId, VipOrderDTO dto) {
        return vipOrderMapper.insert(
                userId,
                dto.getVipLevel(),
                dto.getDurationMonths(),
                dto.getPrice(),
                VipOrderStatus.PENDING
        );
    }

    @Transactional
    public void payOrder(Long orderId) {
        VipOrder order = vipOrderMapper.findById(orderId);
        if (order == null || !VipOrderStatus.PENDING.equals(order.getStatus())) {
            throw new BusinessException("订单不存在或已处理");
        }
        vipOrderMapper.updateStatus(VipOrderStatus.PAID, orderId);

        LocalDateTime expireTime = LocalDateTime.now().plusMonths(order.getDurationMonths());
        userMapper.updateVip(order.getVipLevel(), expireTime, order.getUserId());
    }

    private VipOrderVO toVO(VipOrder order) {
        VipOrderVO vo = new VipOrderVO();
        vo.setId(order.getId());
        vo.setUserId(order.getUserId());
        vo.setVipLevel(order.getVipLevel());
        vo.setDurationMonths(order.getDurationMonths());
        vo.setPrice(order.getPrice());
        vo.setStatus(order.getStatus());
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }
}