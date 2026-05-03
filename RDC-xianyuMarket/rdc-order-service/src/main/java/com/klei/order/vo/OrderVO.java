package com.klei.order.vo;

import com.klei.order.entity.enums.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long buyerId;
    private Long sellerId;
    private Long productId;
    private BigDecimal price;
    private OrderStatus status;
    private LocalDateTime createdAt;
}