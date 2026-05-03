package com.klei.product.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {
    private Long id;
    private Long productId;
    private Long parentId;
    private Long userId;
    private String content;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private List<CommentVO> children;
}