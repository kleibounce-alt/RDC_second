package com.klei.message.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatRecord {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private Long productId;
    private String content;
    private Integer isDeleted;
    private LocalDateTime createdAt;
}