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
import java.util.*;

public class AuthFilter implements Filter {

    private final Gson gson = new Gson();
    // urlPattern -> permissionCode，启动时从数据库加载
    private final Map<String, String> urlPermissionMap = new LinkedHashMap<>();
    // 白名单路径，无需登录即可访问
    private final List<String> whitelist = Arrays.asList("/login", "/register", "/captcha", "/upload");

    @Override
    public void init(FilterConfig filterConfig) {
        loadPermissions();
    }

    private void loadPermissions() {
        String sql = "SELECT url_pattern, code FROM sys_permission WHERE is_deleted = 0 AND url_pattern IS NOT NULL";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String pattern = rs.getString("url_pattern");
                String code = rs.getString("code");
                if (pattern != null && !pattern.isEmpty() && code != null) {
                    urlPermissionMap.put(pattern, code);
                }
            }
            LogUtil.info("AuthFilter 加载权限规则: " + urlPermissionMap.size() + " 条");
        } catch (SQLException e) {
            LogUtil.error("AuthFilter 加载权限配置失败", e);
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        // 去掉 contextPath，得到真实路径
        String path = uri.substring(contextPath.length());

        // 1. 白名单直接放行
        if (isWhitelisted(path)) {
            chain.doFilter(req, resp);
            return;
        }

        // 2. 提取 Token
        String token = JwtUtil.extractToken(request);
        if (token == null) {
            writeJson(response, 401, Result.unauthorized());
            return;
        }

        // 3. 验证 Token 有效性
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

        // 4. 查 Redis 黑名单（RefreshToken 被注销）
        String blacklistKey = "blacklist:refresh:" + userId;
        if (RedisUtil.exists(blacklistKey)) {
            writeJson(response, 401, Result.unauthorized());
            return;
        }

        // 5. URL 粗粒度权限拦截
        String requiredPerm = matchPermission(path);
        if (requiredPerm != null && !userPerms.contains(requiredPerm)) {
            writeJson(response, 403, Result.forbidden());
            return;
        }

        // 6. 把 userId 和权限列表挂到 request，后续 Servlet/Service 可直接取
        request.setAttribute("userId", userId);
        request.setAttribute("permissions", userPerms);

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
            if (pattern.endsWith("*")) {
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