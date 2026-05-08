package com.klei.admin.servlet;

import com.google.gson.reflect.TypeToken;
import com.klei.admin.service.AdminProductService;
import com.klei.admin.service.AdminUserService;
import com.klei.admin.service.SensitiveWordService;
import com.klei.common.annotation.RequireRole;
import com.klei.common.ioc.IoCContainer;
import com.klei.common.servlet.BaseServlet;
import com.klei.common.utils.RedisUtil;
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
        LocalDateTime banEndTime = map.containsKey("banEndTime") && map.get("banEndTime") != null && !map.get("banEndTime").isEmpty()
                ? LocalDateTime.parse(map.get("banEndTime")) : null;
        adminUserService.banUserByUsername(map.get("username"), banEndTime);
        writeJson(resp, Result.ok());
    }

    private void unban(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        adminUserService.unbanUserByUsername(map.get("username"));
        writeJson(resp, Result.ok());
    }

    private void relist(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long productId = Long.valueOf(map.get("productId"));
        adminUserService.relist(productId);
        clearProductCache(productId);
        writeJson(resp, Result.ok());
    }

    private void forceOffShelf(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long productId = Long.valueOf(map.get("productId"));
        adminUserService.forceOffShelf(productId);
        clearProductCache(productId);
        writeJson(resp, Result.ok());
    }

    private void deleteComment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long commentId = Long.valueOf(map.get("commentId"));
        adminUserService.deleteComment(commentId);
        writeJson(resp, Result.ok());
    }

    private void deleteProduct(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long productId = Long.valueOf(map.get("productId"));
        adminProductService.deleteProduct(productId);
        clearProductCache(productId);
        writeJson(resp, Result.ok());
    }

    private void setExposure(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long productId = Long.valueOf(map.get("productId"));
        int level = Integer.parseInt(map.get("level"));
        adminProductService.setExposure(productId, level);
        clearProductCache(productId);
        writeJson(resp, Result.ok());
    }

    private void clearProductCache(Long productId) {
        RedisUtil.del("product:detail:" + productId);
        for (String key : RedisUtil.keys("product:list:*")) {
            RedisUtil.del(key);
        }
    }

    private void listSensitive(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        writeJson(resp, Result.ok(sensitiveWordService.listAll()));
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

    private void offShelved(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        writeJson(resp, Result.ok(adminUserService.findOffShelvedProducts()));
    }
}