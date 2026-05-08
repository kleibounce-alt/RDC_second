package com.klei.common.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.klei.common.exception.AuthException;
import com.klei.common.pool.ConnectionPool;
import com.klei.common.utils.GsonFactory;
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

    private final Gson gson = GsonFactory.get();
    private final Map<String, String> urlPermissionMap = new LinkedHashMap<>();
    private final List<String> whitelist = Arrays.asList(
            "/login", "/register", "/admin-register", "/captcha", "/upload",
            "/forgot-password", "/reset-password", "/refresh-token",
            "/list", "/detail", "/search", "/tag-list",
            "/comment/list", "/user-info", "/user-products",
            "/static"
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

        boolean whitelisted = isWhitelisted(path);

        // WebSocket 升级请求放行，端点自行鉴权
        if ("websocket".equalsIgnoreCase(request.getHeader("Upgrade"))) {
            chain.doFilter(req, resp);
            return;
        }

        // 尝试解析 token，设置用户身份属性（白名单路径不强制要求登录）
        String token = JwtUtil.extractToken(request);
        if (token != null) {
            try {
                DecodedJWT jwt = JwtUtil.verify(token);
                Long userId = jwt.getClaim("userId").asLong();
                String permsStr = jwt.getClaim("perms").asString();
                List<String> userPerms = (permsStr == null || permsStr.isEmpty())
                        ? new ArrayList<>()
                        : Arrays.asList(permsStr.split(","));
                String rolesStr = jwt.getClaim("roles").asString();
                List<String> userRoles = (rolesStr == null || rolesStr.isEmpty())
                        ? new ArrayList<>()
                        : Arrays.asList(rolesStr.split(","));

                // 封禁检查（白名单路径允许只读浏览，不放行则拦截）
                if (!whitelisted) {
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
                            RedisUtil.del("ban:user:" + userId);
                        }
                    }

                    String blacklistKey = "blacklist:access:" + userId;
                    if (RedisUtil.exists(blacklistKey)) {
                        writeJson(response, 401, Result.unauthorized());
                        return;
                    }

                    if (RedisUtil.exists("ban:user:" + userId)) {
                        writeJson(response, 403, Result.fail("账号已被封禁"));
                        return;
                    }
                }

                // 非白名单路径检查权限
                if (!whitelisted) {
                    String requiredPerm = matchPermission(path);
                    if (requiredPerm != null && !userPerms.contains(requiredPerm)) {
                        writeJson(response, 403, Result.forbidden());
                        return;
                    }
                }

                request.setAttribute("userId", userId);
                request.setAttribute("permissions", userPerms);
                request.setAttribute("roles", userRoles);
                request.setAttribute("vipLevel", jwt.getClaim("vipLevel").asInt());
                chain.doFilter(req, resp);
                return;
            } catch (AuthException e) {
                if (!whitelisted) {
                    writeJson(response, 401, Result.unauthorized());
                    return;
                }
            }
        }

        if (!whitelisted) {
            writeJson(response, 401, Result.unauthorized());
            return;
        }

        // 白名单路径且无有效 token，直接放行
        chain.doFilter(req, resp);
    }

    private boolean isWhitelisted(String path) {
        for (String w : whitelist) {
            if (path.equals(w) || path.startsWith(w + "/")) {
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