package com.klei.product.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentLikes {
    private Long userId;
    private Long commentId;
    private Integer isDeleted;
    private LocalDateTime createdAt;
}