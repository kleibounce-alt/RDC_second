package com.klei.message.dto;

import com.klei.message.entity.enums.MessageType;
import lombok.Data;

@Data
public class MsgQueueItem {
    private Long userId;
    private MessageType type;
    private String content;
}