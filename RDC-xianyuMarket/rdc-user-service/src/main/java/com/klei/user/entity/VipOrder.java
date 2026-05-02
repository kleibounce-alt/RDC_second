package com.klei.user.entity;

import com.klei.user.entity.enums.VipOrderStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VipOrder {
    private Long id;
    private Long userId;
    private Integer vipLevel;
    private Integer durationMonths;
    private java.math.BigDecimal price;
    private VipOrderStatus status;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}