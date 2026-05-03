package com.klei.product.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FavoriteVO {
    private Long productId;
    private String productTitle;
    private BigDecimal productPrice;
    private String productImage;
    private LocalDateTime createdAt;
}