package com.klei.message.servlet;

import com.google.gson.reflect.TypeToken;
import com.klei.common.ioc.IoCContainer;
import com.klei.common.mapper.MapperProxyFactory;
import com.klei.common.servlet.BaseServlet;
import com.klei.common.utils.AuthUtil;
import com.klei.common.utils.Result;
import com.klei.message.mapper.ChatRecordMapper;
import com.klei.message.service.MessageService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

@WebServlet(urlPatterns = {"/message/*", "/chat/history"})
public class MessageServlet extends BaseServlet {

    private MessageService messageService;
    private ChatRecordMapper chatRecordMapper;
    private final Type mapType = new TypeToken<Map<String, String>>() {}.getType();

    @Override
    public void init() throws ServletException {
        super.init();
        this.messageService = IoCContainer.getBean(MessageService.class);
        this.chatRecordMapper = MapperProxyFactory.getMapper(ChatRecordMapper.class);
    }

    private void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        writeJson(resp, Result.ok(messageService.findByUserId(userId)));
    }

    private void unreadCount(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        writeJson(resp, Result.ok(messageService.getUnreadCount(userId)));
    }

    private void read(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long messageId = Long.valueOf(map.get("messageId"));
        messageService.markRead(messageId, userId);
        writeJson(resp, Result.ok());
    }

    private void readAll(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        messageService.markAllRead(userId);
        writeJson(resp, Result.ok());
    }

    private void history(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        Long otherId = Long.valueOf(req.getParameter("otherId"));
        writeJson(resp, Result.ok(chatRecordMapper.findDialog(userId, otherId, otherId, userId)));
    }
}