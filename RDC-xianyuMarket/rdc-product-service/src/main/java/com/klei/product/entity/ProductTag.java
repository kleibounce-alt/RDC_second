package com.klei.product.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductTag {
    private Long productId;
    private Long tagId;
    private Integer isDeleted;
    private LocalDateTime createdAt;
}