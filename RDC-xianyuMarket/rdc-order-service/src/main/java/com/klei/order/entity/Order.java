package com.klei.order.entity;

import com.klei.order.entity.enums.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Order {
    private Long id;
    private String orderNo;
    private Long buyerId;
    private Long sellerId;
    private Long productId;
    private BigDecimal price;
    private OrderStatus status;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}