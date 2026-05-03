package com.klei.common.mq;

import com.google.gson.Gson;
import com.klei.common.utils.LogUtil;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MqSender {
    private static final Gson gson = new Gson();

    public static void sendMessage(Long userId, String type, String content) {
        Channel channel = RabbitMQConfig.getChannel();
        if (channel == null) {
            LogUtil.error("MQ Channel 为空，消息发送失败");
            return;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("type", type);
        map.put("content", content);
        String json = gson.toJson(map);
        try {
            channel.basicPublish(
                    RabbitMQConfig.getExchange(),
                    RabbitMQConfig.getRoutingKey(),
                    null,
                    json.getBytes(StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            LogUtil.error("MQ 消息发送失败", e);
        }
    }
}