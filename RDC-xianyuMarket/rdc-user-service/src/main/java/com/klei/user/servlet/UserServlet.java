package com.klei.user.servlet;

import com.google.gson.reflect.TypeToken;
import com.klei.common.ioc.IoCContainer;
import com.klei.common.servlet.BaseServlet;
import com.klei.common.utils.AuthUtil;
import com.klei.common.utils.JwtUtil;
import com.klei.common.utils.Result;
import com.klei.user.dto.*;
import com.klei.user.service.UserService;
import com.klei.user.service.VipService;
import com.klei.user.vo.LoginVO;
import com.klei.user.vo.UserVO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

@WebServlet(urlPatterns = {
        "/login", "/register", "/admin-register", "/logout", "/refresh-token",
        "/forgot-password", "/reset-password", "/profile",
        "/update-profile", "/update-password", "/bind-email",
        "/search-user", "/vip/order", "/vip/pay"
})
public class UserServlet extends BaseServlet {

    private UserService userService;
    private VipService vipService;
    private final Type mapType = new TypeToken<Map<String, String>>() {}.getType();

    @Override
    public void init() throws ServletException {
        super.init();
        this.userService = IoCContainer.getBean(UserService.class);
        this.vipService = IoCContainer.getBean(VipService.class);
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LoginDTO dto = gson.fromJson(req.getReader(), LoginDTO.class);
        LoginVO vo = userService.login(dto);
        writeJson(resp, Result.ok(vo));
    }

    private void register(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RegisterDTO dto = gson.fromJson(req.getReader(), RegisterDTO.class);
        long userId = userService.register(dto);
        writeJson(resp, Result.ok(userId));
    }

    private void adminRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RegisterDTO dto = gson.fromJson(req.getReader(), RegisterDTO.class);
        long userId = userService.registerAdmin(dto);
        writeJson(resp, Result.ok(userId));
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        String token = JwtUtil.extractToken(req);
        userService.logout(userId, token);
        writeJson(resp, Result.ok());
    }

    private void refreshToken(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        LoginVO vo = userService.refreshToken(map.get("refreshToken"));
        writeJson(resp, Result.ok(vo));
    }

    private void forgotPassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        userService.sendResetCode(map.get("username"));
        writeJson(resp, Result.ok());
    }

    private void resetPassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ResetPasswordDTO dto = gson.fromJson(req.getReader(), ResetPasswordDTO.class);
        userService.resetPassword(dto);
        writeJson(resp, Result.ok());
    }

    private void profile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        UserVO vo = userService.findById(userId);
        writeJson(resp, Result.ok(vo));
    }

    private void updateProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        UpdateProfileDTO dto = gson.fromJson(req.getReader(), UpdateProfileDTO.class);
        userService.updateProfile(userId, dto);
        writeJson(resp, Result.ok());
    }

    private void updatePassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        UpdatePasswordDTO dto = gson.fromJson(req.getReader(), UpdatePasswordDTO.class);
        userService.updatePassword(userId, dto);
        writeJson(resp, Result.ok());
    }

    private void bindEmail(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        userService.bindEmail(userId, map.get("email"));
        writeJson(resp, Result.ok());
    }

    private void searchUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String keyword = req.getParameter("keyword");
        writeJson(resp, Result.ok(userService.searchUsers(keyword)));
    }
    
    private void order(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        VipOrderDTO dto = gson.fromJson(req.getReader(), VipOrderDTO.class);
        long orderId = vipService.createOrder(userId, dto);
        writeJson(resp, Result.ok(orderId));
    }

    private void pay(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long orderId = Long.valueOf(map.get("orderId"));
        vipService.payOrder(orderId);
        writeJson(resp, Result.ok());
    }
}