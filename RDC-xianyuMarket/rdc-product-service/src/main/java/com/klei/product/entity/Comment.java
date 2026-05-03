package com.klei.product.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Comment {
    private Long id;
    private Long productId;
    private Long parentId;
    private Long userId;
    private String content;
    private Integer likeCount;
    private Integer isDeleted;
    private LocalDateTime createdAt;
}