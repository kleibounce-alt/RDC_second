package com.klei.user.service.impl;

import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.BusinessException;
import com.klei.order.entity.Wallet;
import com.klei.order.entity.enums.WalletRecordType;
import com.klei.order.mapper.WalletMapper;
import com.klei.order.mapper.WalletRecordMapper;
import com.klei.user.dto.VipOrderDTO;
import com.klei.user.entity.VipOrder;
import com.klei.user.entity.enums.VipOrderStatus;
import com.klei.user.mapper.UserMapper;
import com.klei.user.mapper.VipOrderMapper;
import com.klei.user.service.VipService;
import com.klei.user.vo.VipOrderVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class VipServiceImpl implements VipService {

    @Autowired
    private VipOrderMapper vipOrderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WalletMapper walletMapper;
    @Autowired
    private WalletRecordMapper walletRecordMapper;

    @Override
    public List<VipOrderVO> findByUserId(Long userId) {
        return vipOrderMapper.findByUserId(userId).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
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

    @Override
    @Transactional
    public void payOrder(Long orderId) {
        VipOrder order = vipOrderMapper.findById(orderId);
        if (order == null || !VipOrderStatus.PENDING.equals(order.getStatus())) {
            throw new BusinessException("订单不存在或已处理");
        }

        // 查钱包余额
        Wallet wallet = walletMapper.findByUserId(order.getUserId());
        if (wallet == null) {
            throw new BusinessException("请先充值");
        }
        if (wallet.getBalance().compareTo(order.getPrice()) < 0) {
            throw new BusinessException("余额不足");
        }

        // 扣款
        BigDecimal newBalance = wallet.getBalance().subtract(order.getPrice());
        walletMapper.updateBalance(newBalance, wallet.getId());

        // 记流水
        walletRecordMapper.insert(
                order.getUserId(),
                WalletRecordType.VIP,
                order.getPrice(),
                newBalance,
                "购买VIP等级" + order.getVipLevel()
        );

        // 更新订单状态
        vipOrderMapper.updateStatus(VipOrderStatus.PAID, orderId);

        // 更新用户VIP等级和过期时间
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