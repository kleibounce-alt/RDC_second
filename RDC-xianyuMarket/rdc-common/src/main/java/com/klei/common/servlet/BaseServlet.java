package com.klei.common.servlet;

import com.google.gson.Gson;
import com.klei.common.annotation.RequirePermission;
import com.klei.common.annotation.RequireRole;
import com.klei.common.exception.AuthException;
import com.klei.common.exception.BusinessException;
import com.klei.common.utils.GsonFactory;
import com.klei.common.utils.Result;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public abstract class BaseServlet extends HttpServlet {

    protected final Gson gson = GsonFactory.get();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String httpMethod = req.getMethod().toUpperCase();
        String methodName = "do" + httpMethod.substring(0, 1) + httpMethod.substring(1).toLowerCase();
        Method targetMethod = findMethod(this.getClass(), methodName);

        if (targetMethod != null) {
            checkRole(req, targetMethod);
            checkPermission(req, targetMethod);
        }

        super.service(req, resp);
    }

    /* ========== 默认自动分发，子类无需重写 ========== */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        autoDispatch(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        autoDispatch(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        autoDispatch(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        autoDispatch(req, resp);
    }

    /* ========== 反射分发核心 ========== */
    protected void autoDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String action = resolveAction(req);
        Method method = findActionMethod(action);

        if (method == null) {
            writeJson(resp, Result.fail("未知操作: " + action));
            return;
        }

        try {
            method.setAccessible(true);
            method.invoke(this, req, resp);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException) {
                writeJson(resp, Result.fail(cause.getMessage()));
            } else if (cause instanceof AuthException) {
                AuthException ae = (AuthException) cause;
                resp.setStatus(ae.getCode());
                writeJson(resp, Result.fail(ae.getCode(), ae.getMessage()));
            } else if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw new IOException(cause);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /** 从 action 参数或 URL 路径解析方法名 */
    private String resolveAction(HttpServletRequest req) {
        String action = req.getParameter("action");
        if (action != null && !action.isEmpty()) {
            return action;
        }

        // 优先从 pathInfo 取（适配 /wallet/* 等通配符映射），否则从 servletPath 取
        String pathInfo = req.getPathInfo();
        String path;
        if (pathInfo != null && !pathInfo.isEmpty()) {
            // 多级路径转为横杠连接：/audit/pending -> audit-pending -> auditPending
            path = pathInfo.substring(1).replace('/', '-');
        } else {
            path = req.getServletPath();
            path = path.substring(path.lastIndexOf('/') + 1);
        }
        action = path;
        if (action.isEmpty()) {
            action = "index";
        }
        if (action.contains(".")) {
            action = action.substring(0, action.indexOf('.'));
        }

        // 横杠转驼峰：refresh-token -> refreshToken
        StringBuilder sb = new StringBuilder();
        boolean upperNext = false;
        for (char c : action.toCharArray()) {
            if (c == '-') {
                upperNext = true;
            } else if (upperNext) {
                sb.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 按 action 名找方法：支持 login / doLogin / handleLogin */
    private Method findActionMethod(String action) {
        String[] candidates = {
                action,
                "do" + capitalize(action),
                "handle" + capitalize(action)
        };
        Class<?> clazz = this.getClass();
        for (String name : candidates) {
            try {
                Method m = clazz.getDeclaredMethod(name, HttpServletRequest.class, HttpServletResponse.class);
                if (!m.isSynthetic() && m.getReturnType() == void.class) {
                    return m;
                }
            } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    protected void writeJson(HttpServletResponse resp, Result<?> result) throws IOException {
        resp.getWriter().write(gson.toJson(result));
    }

    /* ========== 原有注解检查逻辑，保持不变 ========== */
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

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.getAttribute("roles");
        if (roles == null) {
            roles = List.of();
        }
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

        @SuppressWarnings("unchecked")
        List<String> perms = (List<String>) req.getAttribute("permissions");
        if (perms == null) {
            perms = List.of();
        }
        List<String> required = Arrays.asList(requirePerm.value());

        boolean has = required.stream().anyMatch(perms::contains);
        if (!has) {
            throw new AuthException(403, "需要权限: " + required);
        }
    }
}