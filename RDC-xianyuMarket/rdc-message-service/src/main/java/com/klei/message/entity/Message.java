package com.klei.message.entity;

import com.klei.message.entity.enums.MessageType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Message {
    private Long id;
    private Long userId;
    private MessageType type;
    private String content;
    private Integer isRead;
    private Integer isDeleted;
    private LocalDateTime createdAt;
}