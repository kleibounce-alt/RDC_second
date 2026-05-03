package com.klei.admin.servlet;

import com.google.gson.reflect.TypeToken;
import com.klei.admin.service.AdminProductService;
import com.klei.admin.service.AdminUserService;
import com.klei.admin.service.SensitiveWordService;
import com.klei.common.annotation.RequireRole;
import com.klei.common.ioc.IoCContainer;
import com.klei.common.servlet.BaseServlet;
import com.klei.common.utils.Result;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Map;

@RequireRole("ROLE_ADMIN")
@WebServlet(urlPatterns = {"/admin/*"})
public class AdminServlet extends BaseServlet {

    private AdminUserService adminUserService;
    private AdminProductService adminProductService;
    private SensitiveWordService sensitiveWordService;
    private final Type mapType = new TypeToken<Map<String, String>>() {}.getType();

    @Override
    public void init() throws ServletException {
        super.init();
        this.adminUserService = IoCContainer.getBean(AdminUserService.class);
        this.adminProductService = IoCContainer.getBean(AdminProductService.class);
        this.sensitiveWordService = IoCContainer.getBean(SensitiveWordService.class);
    }

    private void ban(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long userId = Long.valueOf(map.get("userId"));
        LocalDateTime banEndTime = LocalDateTime.parse(map.get("banEndTime"));
        adminUserService.banUser(userId, banEndTime);
        writeJson(resp, Result.ok());
    }

    private void unban(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long userId = Long.valueOf(map.get("userId"));
        adminUserService.unbanUser(userId);
        writeJson(resp, Result.ok());
    }

    private void forceOffShelf(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long productId = Long.valueOf(map.get("productId"));
        adminUserService.forceOffShelf(productId);
        writeJson(resp, Result.ok());
    }

    private void deleteComment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long commentId = Long.valueOf(map.get("commentId"));
        adminUserService.deleteComment(commentId);
        writeJson(resp, Result.ok());
    }

    private void exposure(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long productId = Long.valueOf(map.get("productId"));
        Integer weight = Integer.valueOf(map.get("weight"));
        adminProductService.increaseExposure(productId, weight);
        writeJson(resp, Result.ok());
    }

    private void deleteProduct(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long productId = Long.valueOf(map.get("productId"));
        adminProductService.deleteProduct(productId);
        writeJson(resp, Result.ok());
    }

    private void addSensitive(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        String word = map.get("word");
        sensitiveWordService.addWord(word);
        writeJson(resp, Result.ok());
    }

    private void deleteSensitive(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long id = Long.valueOf(map.get("id"));
        sensitiveWordService.deleteWord(id);
        writeJson(resp, Result.ok());
    }
}