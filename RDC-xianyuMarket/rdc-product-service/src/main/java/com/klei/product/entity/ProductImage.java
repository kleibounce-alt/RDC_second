package com.klei.product.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductImage {
    private Long id;
    private Long productId;
    private String imageUrl;
    private Integer sort;
    private Integer isDeleted;
    private LocalDateTime createdAt;
}