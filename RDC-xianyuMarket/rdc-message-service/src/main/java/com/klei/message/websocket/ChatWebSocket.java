package com.klei.message.websocket;

import com.google.gson.Gson;
import com.klei.common.ioc.IoCContainer;
import com.klei.common.mapper.MapperProxyFactory;
import com.klei.common.mq.MqSender;
import com.klei.common.pool.ConnectionPool;
import com.klei.common.utils.GsonFactory;
import com.klei.common.utils.JwtUtil;
import com.klei.common.utils.RedisUtil;
import com.klei.message.mapper.ChatRecordMapper;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/chat")
public class ChatWebSocket {

    private static final Gson gson = GsonFactory.get();
    private static final Map<Long, Session> onlineUsers = new ConcurrentHashMap<>();
    private static final Map<Long, String[]> userInfoCache = new ConcurrentHashMap<>();

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

            // 缓存用户昵称和头像
            if (!userInfoCache.containsKey(this.userId)) {
                String[] info = loadUserInfo(this.userId);
                userInfoCache.put(this.userId, info);
            }

            this.chatRecordMapper = IoCContainer.getBean(ChatRecordMapper.class);
            if (this.chatRecordMapper == null) {
                this.chatRecordMapper = MapperProxyFactory.getMapper(ChatRecordMapper.class);
            }

            // 通知所有在线用户：该用户上线
            broadcastStatus(this.userId, true);
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

        // 缓存接收者信息
        if (!userInfoCache.containsKey(msg.receiverId)) {
            String[] info = loadUserInfo(msg.receiverId);
            userInfoCache.put(msg.receiverId, info);
        }
        String[] senderInfo = userInfoCache.getOrDefault(this.userId, new String[]{"", ""});

        MqSender.sendMessage(msg.receiverId, "SYSTEM",
                "CHAT|" + this.userId + "|您收到一条新聊天消息");

        ChatResp resp = new ChatResp(this.userId, msg.receiverId, msg.productId, filtered,
                senderInfo[0], senderInfo[1]);

        // 发送给接收者
        Session target = onlineUsers.get(msg.receiverId);
        if (target != null && target.isOpen()) {
            try {
                target.getBasicRemote().sendText(gson.toJson(resp));
            } catch (IOException ignored) {}
        }

        // 回显给发送者
        try {
            session.getBasicRemote().sendText(gson.toJson(resp));
        } catch (IOException ignored) {}
    }

    @OnClose
    public void onClose(Session session) {
        if (this.userId != null) {
            onlineUsers.remove(this.userId);
            broadcastStatus(this.userId, false);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        if (this.userId != null) {
            onlineUsers.remove(this.userId);
            broadcastStatus(this.userId, false);
        }
    }

    private void broadcastStatus(Long userId, boolean online) {
        String json = gson.toJson(new StatusMsg(userId, online));
        for (Session s : onlineUsers.values()) {
            if (s.isOpen()) {
                try {
                    s.getBasicRemote().sendText(json);
                } catch (IOException ignored) {}
            }
        }
    }

    private String[] loadUserInfo(Long userId) {
        String sql = "SELECT nickname, avatar FROM sys_user WHERE id = ? AND is_deleted = 0";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nickname = rs.getString("nickname");
                    String avatar = rs.getString("avatar");
                    return new String[]{
                            nickname != null ? nickname : "",
                            avatar != null ? avatar : ""
                    };
                }
            }
        } catch (SQLException ignored) {}
        return new String[]{"", ""};
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
        String type = "msg";
        Long senderId;
        Long receiverId;
        Long productId;
        String content;
        String createdAt;
        String senderNickname;
        String senderAvatar;

        ChatResp(Long senderId, Long receiverId, Long productId, String content,
                 String senderNickname, String senderAvatar) {
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.productId = productId;
            this.content = content;
            this.createdAt = java.time.LocalDateTime.now().toString();
            this.senderNickname = senderNickname;
            this.senderAvatar = senderAvatar;
        }
    }

    private static class StatusMsg {
        String type = "status";
        Long userId;
        boolean online;

        StatusMsg(Long userId, boolean online) {
            this.userId = userId;
            this.online = online;
        }
    }
}
