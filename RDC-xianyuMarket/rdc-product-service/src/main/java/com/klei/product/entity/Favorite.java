package com.klei.product.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Favorite {
    private Long userId;
    private Long productId;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}