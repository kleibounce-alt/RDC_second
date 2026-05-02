package com.klei.product.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Follow {
    private Long userId;
    private Long followUserId;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}