package com.klei.user.servlet;

import com.google.gson.reflect.TypeToken;
import com.klei.common.ioc.IoCContainer;
import com.klei.common.servlet.BaseServlet;
import com.klei.common.utils.AuthUtil;
import com.klei.common.utils.FileUploadUtil;
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
        "/search-user", "/user-info", "/vip/order", "/vip/pay", "/upload-avatar"
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
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            writeJson(resp, Result.fail("登录仅支持POST请求，当前: " + req.getMethod()));
            return;
        }
        LoginDTO dto = gson.fromJson(req.getReader(), LoginDTO.class);
        if (dto == null) {
            writeJson(resp, Result.fail("请求体为空"));
            return;
        }
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

    private void userInfo(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = Long.valueOf(req.getParameter("userId"));
        com.klei.user.vo.UserVO vo = userService.findById(userId);
        if (vo == null) {
            writeJson(resp, Result.fail("用户不存在"));
            return;
        }
        vo.setEmail(null);
        vo.setPhone(null);
        writeJson(resp, Result.ok(vo));
    }

    private void order(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        VipOrderDTO dto = gson.fromJson(req.getReader(), VipOrderDTO.class);
        long orderId = vipService.createOrder(userId, dto);
        writeJson(resp, Result.ok(orderId));
    }

    private void uploadAvatar(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Long userId = AuthUtil.getUserId(req);
        if (userId == null) {
            writeJson(resp, Result.fail("请先登录"));
            return;
        }
        String path;
        try {
            path = FileUploadUtil.uploadSingleImage(req, "avatar");
        } catch (Exception e) {
            writeJson(resp, Result.fail("头像上传失败: " + e.getMessage()));
            return;
        }
        if (path == null) {
            writeJson(resp, Result.fail("未检测到上传文件"));
            return;
        }
        UserVO current = userService.findById(userId);
        if (current == null) {
            writeJson(resp, Result.fail("用户不存在"));
            return;
        }
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setNickname(current.getNickname());
        dto.setAvatar("/u/" + path);
        dto.setEmail(current.getEmail());
        dto.setPhone(current.getPhone());
        userService.updateProfile(userId, dto);
        writeJson(resp, Result.ok("/u/" + path));
    }

    private void pay(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long orderId = Long.valueOf(map.get("orderId"));
        vipService.payOrder(orderId);
        writeJson(resp, Result.ok());
    }
}