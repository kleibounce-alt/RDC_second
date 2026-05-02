package com.klei.order.entity;

import com.klei.order.entity.enums.WalletRecordType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletRecord {
    private Long id;
    private Long userId;
    private WalletRecordType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String remark;
    private Integer isDeleted;
    private LocalDateTime createdAt;
}