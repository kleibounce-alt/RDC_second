package com.klei.common.servlet;

import com.klei.common.annotation.RequirePermission;
import com.klei.common.annotation.RequireRole;
import com.klei.common.exception.AuthException;
import com.klei.common.utils.JwtUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public abstract class BaseServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 根据 HTTP 方法找子类重写的方法
        String httpMethod = req.getMethod().toUpperCase();
        String methodName = "do" + httpMethod.substring(0, 1) + httpMethod.substring(1).toLowerCase();
        Method targetMethod = findMethod(this.getClass(), methodName);

        // 解析并校验注解
        if (targetMethod != null) {
            checkRole(req, targetMethod);
            checkPermission(req, targetMethod);
        }

        super.service(req, resp);
    }

    private Method findMethod(Class<?> clazz, String name) {
        while (clazz != null && clazz != HttpServlet.class) {
            try {
                return clazz.getDeclaredMethod(name, HttpServletRequest.class, HttpServletResponse.class);
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private void checkRole(HttpServletRequest req, Method method) {
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = this.getClass().getAnnotation(RequireRole.class);
        }
        if (requireRole == null) {
            return;
        }

        String token = JwtUtil.extractToken(req);
        List<String> roles = JwtUtil.getRoles(token);
        List<String> required = Arrays.asList(requireRole.value());

        boolean has = required.stream().anyMatch(roles::contains);
        if (!has) {
            throw new AuthException(403, "需要角色: " + required);
        }
    }

    private void checkPermission(HttpServletRequest req, Method method) {
        RequirePermission requirePerm = method.getAnnotation(RequirePermission.class);
        if (requirePerm == null) {
            requirePerm = this.getClass().getAnnotation(RequirePermission.class);
        }
        if (requirePerm == null) {
            return;
        }

        String token = JwtUtil.extractToken(req);
        List<String> perms = JwtUtil.getPermissions(token);
        List<String> required = Arrays.asList(requirePerm.value());

        boolean has = required.stream().anyMatch(perms::contains);
        if (!has) {
            throw new AuthException(403, "需要权限: " + required);
        }
    }
}