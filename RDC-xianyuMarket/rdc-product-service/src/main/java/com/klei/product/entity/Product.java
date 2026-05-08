package com.klei.product.entity;

import com.klei.product.entity.enums.ProductStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Product {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private ProductStatus status;
    private Integer version;
    private Integer viewCount;
    private Integer exposureWeight;
    private String rejectReason;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductImage> images;
    private Boolean isFavorited;
}