package com.klei.order.service;

import com.klei.order.vo.WalletRecordVO;
import com.klei.order.vo.WalletVO;
import java.math.BigDecimal;
import java.util.List;

public interface WalletService {

    void recharge(Long userId, BigDecimal amount);

    WalletVO getBalance(Long userId);

    List<WalletRecordVO> getRecords(Long userId);
}