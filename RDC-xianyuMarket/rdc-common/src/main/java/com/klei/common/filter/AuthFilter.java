package com.klei.common.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.klei.common.exception.AuthException;
import com.klei.common.pool.ConnectionPool;
import com.klei.common.utils.JwtUtil;
import com.klei.common.utils.LogUtil;
import com.klei.common.utils.RedisUtil;
import com.klei.common.utils.Result;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class AuthFilter implements Filter {

    private final Gson gson = new Gson();
    private final Map<String, String> urlPermissionMap = new LinkedHashMap<>();
    private final List<String> whitelist = Arrays.asList(
            "/login", "/register", "/captcha", "/upload",
            "/forgot-password", "/reset-password", "/refresh-token"
    );

    @Override
    public void init(FilterConfig filterConfig) {
        loadPermissions();
    }

    private void loadPermissions() {
        String sql = "SELECT url_pattern, code FROM sys_permission WHERE is_deleted = 0 AND url_pattern IS NOT NULL";
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String pattern = rs.getString("url_pattern");
                String code = rs.getString("code");
                if (pattern != null && !pattern.isEmpty() && code != null) {
                    urlPermissionMap.put(pattern, code);
                }
            }
            rs.close();
            ps.close();
            LogUtil.info("AuthFilter 加载权限规则: " + urlPermissionMap.size() + " 条");
        } catch (SQLException e) {
            LogUtil.error("AuthFilter 加载权限配置失败", e);
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());

        if (isWhitelisted(path)) {
            chain.doFilter(req, resp);
            return;
        }

        String token = JwtUtil.extractToken(request);
        if (token == null) {
            writeJson(response, 401, Result.unauthorized());
            return;
        }

        DecodedJWT jwt;
        try {
            jwt = JwtUtil.verify(token);
        } catch (AuthException e) {
            writeJson(response, 401, Result.unauthorized());
            return;
        }

        Long userId = jwt.getClaim("userId").asLong();
        String permsStr = jwt.getClaim("perms").asString();
        List<String> userPerms = (permsStr == null || permsStr.isEmpty())
                ? new ArrayList<>()
                : Arrays.asList(permsStr.split(","));
        String rolesStr = jwt.getClaim("roles").asString();
        List<String> userRoles = (rolesStr == null || rolesStr.isEmpty())
                ? new ArrayList<>()
                : Arrays.asList(rolesStr.split(","));

        // ===== 封禁检查：未到期拦截，已过期自动解封 =====
        Date banEndTimeClaim = jwt.getClaim("banEndTime").asDate();
        if (banEndTimeClaim != null) {
            LocalDateTime banEnd = new java.sql.Timestamp(banEndTimeClaim.getTime()).toLocalDateTime();
            if (LocalDateTime.now().isBefore(banEnd)) {
                writeJson(response, 403, Result.fail("账号封禁中，解封时间：" + banEnd));
                return;
            } else {
                try (Connection conn = ConnectionPool.getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "UPDATE sys_user SET status = 'NORMAL', ban_end_time = NULL, updated_at = NOW() WHERE id = ?")) {
                    ps.setLong(1, userId);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    LogUtil.error("自动解封用户失败 uid=" + userId, e);
                }
            }
        }
        // =================================================

        String blacklistKey = "blacklist:access:" + userId;
        if (RedisUtil.exists(blacklistKey)) {
            writeJson(response, 401, Result.unauthorized());
            return;
        }

        String requiredPerm = matchPermission(path);
        if (requiredPerm != null && !userPerms.contains(requiredPerm)) {
            writeJson(response, 403, Result.forbidden());
            return;
        }

        request.setAttribute("userId", userId);
        request.setAttribute("permissions", userPerms);
        request.setAttribute("roles", userRoles);
        request.setAttribute("vipLevel", jwt.getClaim("vipLevel").asInt());
        chain.doFilter(req, resp);
    }

    private boolean isWhitelisted(String path) {
        for (String w : whitelist) {
            if (path.startsWith(w)) {
                return true;
            }
        }
        return false;
    }

    private String matchPermission(String path) {
        for (Map.Entry<String, String> entry : urlPermissionMap.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.endsWith("/*")) {
                String prefix = pattern.substring(0, pattern.length() - 1);
                if (path.startsWith(prefix)) {
                    return entry.getValue();
                }
            } else if (pattern.equals(path)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void writeJson(HttpServletResponse response, int status, Result<?> result) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write(gson.toJson(result));
        out.flush();
    }

    @Override
    public void destroy() {}
}