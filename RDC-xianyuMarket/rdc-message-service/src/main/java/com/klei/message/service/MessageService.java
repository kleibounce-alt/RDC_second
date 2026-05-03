package com.klei.message.service;

import com.klei.message.entity.enums.MessageType;
import com.klei.message.vo.MessageVO;
import java.util.List;

public interface MessageService {

    List<MessageVO> findByUserId(Long userId);

    int getUnreadCount(Long userId);

    void markRead(Long messageId, Long userId);

    void markAllRead(Long userId);

    void sendMessage(Long userId, MessageType type, String content);

    void flushQueue();
}