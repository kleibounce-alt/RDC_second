package com.klei.order.vo;

import com.klei.order.entity.enums.WalletRecordType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletRecordVO {
    private Long id;
    private WalletRecordType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String remark;
    private LocalDateTime createdAt;
}