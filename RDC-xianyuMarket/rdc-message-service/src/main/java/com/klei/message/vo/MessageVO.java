package com.klei.message.vo;

import com.klei.message.entity.enums.MessageType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageVO {
    private Long id;
    private MessageType type;
    private String content;
    private Integer isRead;
    private LocalDateTime createdAt;
}