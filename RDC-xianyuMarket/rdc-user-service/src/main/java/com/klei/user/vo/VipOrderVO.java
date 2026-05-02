package com.klei.user.vo;

import com.klei.user.entity.enums.VipOrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VipOrderVO {
    private Long id;
    private Long userId;
    private Integer vipLevel;
    private Integer durationMonths;
    private BigDecimal price;
    private VipOrderStatus status;
    private LocalDateTime createdAt;
}