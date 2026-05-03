package com.klei.common.mq;

import com.klei.common.utils.LogUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class RabbitMQConfig {

    private static final String HOST = "localhost";
    private static final int PORT = 5672;
    private static final String USER = "guest";
    private static final String PASS = "guest";

    // 主交换机和队列
    private static final String EXCHANGE = "xianyu.direct";
    private static final String QUEUE = "msg.queue";
    private static final String ROUTING_KEY = "msg";

    // 死信交换机和队列
    private static final String DLX_EXCHANGE = "xianyu.dlx";
    private static final String DLX_QUEUE = "msg.dlq";
    private static final String DLX_ROUTING_KEY = "msg.dlq";

    private static Connection connection;
    private static Channel channel;

    static {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(HOST);
            factory.setPort(PORT);
            factory.setUsername(USER);
            factory.setPassword(PASS);
            connection = factory.newConnection();
            channel = connection.createChannel();

            // 死信交换机
            channel.exchangeDeclare(DLX_EXCHANGE, "direct", true);
            channel.queueDeclare(DLX_QUEUE, true, false, false, null);
            channel.queueBind(DLX_QUEUE, DLX_EXCHANGE, DLX_ROUTING_KEY);

            // 主队列绑定死信参数
            Map<String, Object> queueArgs = new HashMap<>();
            queueArgs.put("x-dead-letter-exchange", DLX_EXCHANGE);
            queueArgs.put("x-dead-letter-routing-key", DLX_ROUTING_KEY);

            channel.exchangeDeclare(EXCHANGE, "direct", true);
            channel.queueDeclare(QUEUE, true, false, false, queueArgs);
            channel.queueBind(QUEUE, EXCHANGE, ROUTING_KEY);

            LogUtil.info("RabbitMQ 连接成功，死信队列已配置");
        } catch (Exception e) {
            LogUtil.error("RabbitMQ 初始化失败", e);
        }
    }

    public static Channel getChannel() {
        return channel;
    }

    public static String getExchange() {
        return EXCHANGE;
    }

    public static String getQueue() {
        return QUEUE;
    }

    public static String getRoutingKey() {
        return ROUTING_KEY;
    }

    public static String getDlxExchange() {
        return DLX_EXCHANGE;
    }

    public static String getDlxQueue() {
        return DLX_QUEUE;
    }

    public static String getDlxRoutingKey() {
        return DLX_ROUTING_KEY;
    }

    public static void close() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            LogUtil.error("RabbitMQ 关闭失败", e);
        }
    }
}