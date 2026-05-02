package com.klei.product.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Tag {
    private Long id;
    private String name;
    private Integer isDeleted;
    private LocalDateTime createdAt;
}