package com.klei.order.service.impl;

import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.BusinessException;
import com.klei.order.entity.Wallet;
import com.klei.order.entity.WalletRecord;
import com.klei.order.entity.enums.WalletRecordType;
import com.klei.order.mapper.WalletMapper;
import com.klei.order.mapper.WalletRecordMapper;
import com.klei.order.service.WalletService;
import com.klei.order.vo.WalletRecordVO;
import com.klei.order.vo.WalletVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WalletServiceImpl implements WalletService {

    @Autowired
    private WalletMapper walletMapper;
    @Autowired
    private WalletRecordMapper walletRecordMapper;

    private static final BigDecimal MAX_RECHARGE = new BigDecimal("99999999.99");

    @Override
    @Transactional
    public void recharge(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于0");
        }
        if (amount.compareTo(MAX_RECHARGE) > 0) {
            throw new BusinessException("单次充值不能超过99,999,999.99元");
        }

        Wallet wallet = walletMapper.findByUserId(userId);
        if (wallet == null) {
            walletMapper.insert(userId);
            wallet = walletMapper.findByUserId(userId);
        }

        BigDecimal newBalance = wallet.getBalance().add(amount);
        walletMapper.updateBalance(newBalance, wallet.getId());

        walletRecordMapper.insert(userId, WalletRecordType.RECHARGE, amount, newBalance, "余额充值");
    }

    @Override
    public WalletVO getBalance(Long userId) {
        Wallet wallet = walletMapper.findByUserId(userId);
        if (wallet == null) {
            walletMapper.insert(userId);
            wallet = walletMapper.findByUserId(userId);
        }

        WalletVO vo = new WalletVO();
        vo.setUserId(userId);
        vo.setBalance(wallet.getBalance());
        return vo;
    }

    @Override
    public List<WalletRecordVO> getRecords(Long userId) {
        return walletRecordMapper.findByUserId(userId).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    private WalletRecordVO toVO(WalletRecord r) {
        WalletRecordVO vo = new WalletRecordVO();
        vo.setId(r.getId());
        vo.setType(r.getType());
        vo.setAmount(r.getAmount());
        vo.setBalanceAfter(r.getBalanceAfter());
        vo.setRemark(r.getRemark());
        vo.setCreatedAt(r.getCreatedAt());
        return vo;
    }
}