package com.klei.product.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductPublishDTO {
    private String title;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private List<String> images;
    private List<String> tags;
}