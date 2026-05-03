package com.klei.order.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WalletVO {
    private Long userId;
    private BigDecimal balance;
}