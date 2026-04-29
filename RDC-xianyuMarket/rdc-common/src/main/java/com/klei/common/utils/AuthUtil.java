package com.klei.common.utils;

import com.klei.common.exception.AuthException;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

public class AuthUtil {

    public static Long getUserId(HttpServletRequest req) {
        return (Long) req.getAttribute("userId");
    }

    @SuppressWarnings("unchecked")
    public static List<String> getRoles(HttpServletRequest req) {
        return (List<String>) req.getAttribute("roles");
    }

    @SuppressWarnings("unchecked")
    public static List<String> getPermissions(HttpServletRequest req) {
        return (List<String>) req.getAttribute("permissions");
    }

    public static void checkRole(HttpServletRequest req, String... requiredRoles) {
        List<String> roles = getRoles(req);
        boolean has = Arrays.stream(requiredRoles).anyMatch(roles::contains);
        if (!has) {
            throw new AuthException(403, "需要角色: " + Arrays.toString(requiredRoles));
        }
    }

    public static void checkPermission(HttpServletRequest req, String... requiredPerms) {
        List<String> perms = getPermissions(req);
        boolean has = Arrays.stream(requiredPerms).anyMatch(perms::contains);
        if (!has) {
            throw new AuthException(403, "需要权限: " + Arrays.toString(requiredPerms));
        }
    }
}