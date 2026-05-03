package com.klei.message.websocket;

import com.google.gson.Gson;
import com.klei.common.mapper.MapperProxyFactory;
import com.klei.common.utils.JwtUtil;
import com.klei.common.utils.RedisUtil;
import com.klei.message.mapper.ChatRecordMapper;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/chat")
public class ChatWebSocket {

    private static final Gson gson = new Gson();
    private static final Map<Long, Session> onlineUsers = new ConcurrentHashMap<>();

    private Long userId;
    private ChatRecordMapper chatRecordMapper;

    @OnOpen
    public void onOpen(Session session) {
        String token = getQueryParam(session, "token");
        if (token == null) {
            close(session);
            return;
        }
        try {
            this.userId = JwtUtil.getUserId(token);
            onlineUsers.put(this.userId, session);
            this.chatRecordMapper = MapperProxyFactory.getMapper(ChatRecordMapper.class);
        } catch (Exception e) {
            close(session);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        ChatMsg msg = gson.fromJson(message, ChatMsg.class);
        if (msg == null || msg.receiverId == null || msg.content == null || msg.content.trim().isEmpty()) {
            return;
        }

        String filtered = filter(msg.content.trim());

        chatRecordMapper.insert(this.userId, msg.receiverId, msg.productId, filtered);

        Session target = onlineUsers.get(msg.receiverId);
        if (target != null && target.isOpen()) {
            try {
                target.getBasicRemote().sendText(
                        gson.toJson(new ChatResp(this.userId, msg.productId, filtered))
                );
            } catch (IOException ignored) {}
        }
    }

    @OnClose
    public void onClose(Session session) {
        if (this.userId != null) {
            onlineUsers.remove(this.userId);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        if (this.userId != null) {
            onlineUsers.remove(this.userId);
        }
    }

    private String filter(String content) {
        Set<String> words = RedisUtil.smembers("sensitive:words");
        if (words == null || words.isEmpty()) {
            return content;
        }
        String result = content;
        for (String word : words) {
            if (word != null && !word.isEmpty() && result.contains(word)) {
                result = result.replace(word, "*".repeat(word.length()));
            }
        }
        return result;
    }

    private String getQueryParam(Session session, String key) {
        String query = session.getRequestURI().getQuery();
        if (query == null) {
            return null;
        }
        for (String param : query.split("&")) {
            String[] kv = param.split("=");
            if (kv.length == 2 && kv[0].equals(key)) {
                return kv[1];
            }
        }
        return null;
    }

    private void close(Session session) {
        try { session.close(); } catch (IOException ignored) {}
    }

    private static class ChatMsg {
        Long receiverId;
        Long productId;
        String content;
    }

    private static class ChatResp {
        Long senderId;
        Long productId;
        String content;

        ChatResp(Long senderId, Long productId, String content) {
            this.senderId = senderId;
            this.productId = productId;
            this.content = content;
        }
    }
}