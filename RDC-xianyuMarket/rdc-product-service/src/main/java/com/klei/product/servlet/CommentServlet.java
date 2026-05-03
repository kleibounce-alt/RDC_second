package com.klei.product.servlet;

import com.google.gson.reflect.TypeToken;
import com.klei.common.ioc.IoCContainer;
import com.klei.common.servlet.BaseServlet;
import com.klei.common.utils.AuthUtil;
import com.klei.common.utils.Result;
import com.klei.product.service.CommentService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

@WebServlet(urlPatterns = {"/comment/*"})
public class CommentServlet extends BaseServlet {

    private CommentService commentService;
    private final Type mapType = new TypeToken<Map<String, String>>() {}.getType();

    @Override
    public void init() throws ServletException {
        super.init();
        this.commentService = IoCContainer.getBean(CommentService.class);
    }

    private void add(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long productId = Long.valueOf(map.get("productId"));
        String content = map.get("content");
        long id = commentService.addComment(productId, userId, content);
        writeJson(resp, Result.ok(id));
    }

    private void reply(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long productId = Long.valueOf(map.get("productId"));
        Long parentId = Long.valueOf(map.get("parentId"));
        String content = map.get("content");
        long id = commentService.reply(productId, parentId, userId, content);
        writeJson(resp, Result.ok(id));
    }

    private void delete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long commentId = Long.valueOf(map.get("commentId"));
        commentService.deleteComment(commentId, userId);
        writeJson(resp, Result.ok());
    }

    private void like(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long commentId = Long.valueOf(map.get("commentId"));
        commentService.toggleLike(commentId, userId);
        writeJson(resp, Result.ok());
    }

    private void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long productId = Long.valueOf(req.getParameter("productId"));
        writeJson(resp, Result.ok(commentService.findTreeByProductId(productId)));
    }
}