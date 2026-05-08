package com.klei.message.service;

import com.klei.common.vo.PageResult;
import com.klei.message.entity.enums.MessageType;
import com.klei.message.vo.MessageVO;

public interface MessageService {

    PageResult<MessageVO> findByUserId(Long userId, int page, int size);

    int getUnreadCount(Long userId);

    void markRead(Long messageId, Long userId);

    void markAllRead(Long userId);

    void sendMessage(Long userId, MessageType type, String content);

    void flushQueue();
}