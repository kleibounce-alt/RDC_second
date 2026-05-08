package com.klei.message.service.impl;

import com.google.gson.Gson;
import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Transactional;
import com.klei.common.utils.GsonFactory;
import com.klei.common.annotation.Component;
import com.klei.common.exception.BusinessException;
import com.klei.common.mq.RabbitMQConfig;
import com.klei.common.utils.LogUtil;
import com.klei.common.vo.PageResult;
import com.klei.message.dto.MsgQueueItem;
import com.klei.message.entity.Message;
import com.klei.message.entity.enums.MessageType;
import com.klei.message.mapper.MessageMapper;
import com.klei.message.service.MessageService;
import com.klei.message.vo.MessageVO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    private final Gson gson = GsonFactory.get();

    @Override
    public PageResult<MessageVO> findByUserId(Long userId, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        int offset = (page - 1) * size;
        PageResult<MessageVO> result = new PageResult<>();
        result.setList(messageMapper.findByUserIdPage(userId, offset, size).stream()
                .map(this::toVO).collect(Collectors.toList()));
        result.setTotal(messageMapper.countByUserId(userId));
        result.setPage(page);
        result.setSize(size);
        return result;
    }

    @Override
    @Transactional
    public int getUnreadCount(Long userId) {
        return messageMapper.findUnreadByUserId(userId).size();
    }

    @Override
    public void markRead(Long messageId, Long userId) {
        Message msg = messageMapper.findById(messageId);
        if (msg == null || msg.getIsDeleted() == 1) {
            throw new BusinessException("消息不存在");
        }
        if (!msg.getUserId().equals(userId)) {
            throw new BusinessException("无权操作");
        }
        messageMapper.markRead(messageId);
    }

    @Override
    public void markAllRead(Long userId) {
        messageMapper.markAllRead(userId);
    }

    @Override
    public void sendMessage(Long userId, MessageType type, String content) {
        MsgQueueItem item = new MsgQueueItem();
        item.setUserId(userId);
        item.setType(type);
        item.setContent(content);
        String json = gson.toJson(item);
        try {
            RabbitMQConfig.getChannel().basicPublish(
                    RabbitMQConfig.getExchange(),
                    RabbitMQConfig.getRoutingKey(),
                    null,
                    json.getBytes(StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            LogUtil.error("RabbitMQ 消息发送失败", e);
            throw new RuntimeException("消息发送失败", e);
        }
    }

    @Override
    public void flushQueue() {
        // RabbitMQ 模式下由消费者实时消费，无需手动 flush
    }

    private MessageVO toVO(Message m) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setType(m.getType());
        vo.setContent(m.getContent());
        vo.setIsRead(m.getIsRead());
        vo.setCreatedAt(m.getCreatedAt());
        return vo;
    }
}