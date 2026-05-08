package com.klei.message.mq;

import com.google.gson.Gson;
import com.klei.common.annotation.Autowired;
import com.klei.common.utils.GsonFactory;
import com.klei.common.annotation.Component;
import com.klei.common.mq.RabbitMQConfig;
import com.klei.common.utils.LogUtil;
import com.klei.message.dto.MsgQueueItem;
import com.klei.message.mapper.MessageMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class MessageConsumer {

    @Autowired
    private MessageMapper messageMapper;

    private final Gson gson = GsonFactory.get();
    private static final int MAX_RETRY = 2; // 最多重试 3 次（0,1,2）

    public void start() {
        Channel channel = RabbitMQConfig.getChannel();
        if (channel == null) {
            LogUtil.error("RabbitMQ Channel 为空，消费者无法启动");
            return;
        }
        try {
            channel.basicQos(1);
            channel.basicConsume(RabbitMQConfig.getQueue(), false, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String json = new String(body, StandardCharsets.UTF_8);

                    // 读取重试次数
                    int retryCount = 0;
                    if (properties.getHeaders() != null && properties.getHeaders().get("x-retry-count") != null) {
                        Object rc = properties.getHeaders().get("x-retry-count");
                        if (rc instanceof Number) {
                            retryCount = ((Number) rc).intValue();
                        }
                    }

                    try {
                        MsgQueueItem item = gson.fromJson(json, MsgQueueItem.class);
                        messageMapper.insert(item.getUserId(), item.getType(), item.getContent());
                        channel.basicAck(envelope.getDeliveryTag(), false);
                    } catch (Exception e) {
                        LogUtil.error("消息消费失败，当前重试次数: " + retryCount + " | " + json, e);

                        if (retryCount < MAX_RETRY) {
                            // 重新发布，重试次数 +1
                            Map<String, Object> newHeaders = new HashMap<>();
                            newHeaders.put("x-retry-count", retryCount + 1);

                            AMQP.BasicProperties newProps = new AMQP.BasicProperties.Builder()
                                    .headers(newHeaders)
                                    .build();

                            try {
                                channel.basicPublish(
                                        RabbitMQConfig.getExchange(),
                                        RabbitMQConfig.getRoutingKey(),
                                        newProps,
                                        body
                                );
                                // 原消息 ack，避免重复
                                channel.basicAck(envelope.getDeliveryTag(), false);
                            } catch (IOException ex) {
                                LogUtil.error("重试消息重新发布失败", ex);
                                channel.basicNack(envelope.getDeliveryTag(), false, true);
                            }
                        } else {
                            // 超过最大重试次数，进入死信队列
                            channel.basicNack(envelope.getDeliveryTag(), false, false);
                            LogUtil.error("消息进入死信队列: " + json);
                        }
                    }
                }
            });
            LogUtil.info("RabbitMQ 消息消费者已启动，最大重试次数: " + (MAX_RETRY + 1));
        } catch (IOException e) {
            LogUtil.error("RabbitMQ 消费者启动失败", e);
        }
    }
}