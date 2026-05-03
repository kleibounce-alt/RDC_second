package com.klei.admin.servlet;

import com.google.gson.reflect.TypeToken;
import com.klei.admin.service.AuditService;
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
import java.util.Map;

@RequireRole("ROLE_ADMIN")
@WebServlet(urlPatterns = {"/admin/audit/*"})
public class AuditServlet extends BaseServlet {

    private AuditService auditService;
    private final Type mapType = new TypeToken<Map<String, String>>() {}.getType();

    @Override
    public void init() throws ServletException {
        super.init();
        this.auditService = IoCContainer.getBean(AuditService.class);
    }

    private void pending(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        writeJson(resp, Result.ok(auditService.findPendingProducts()));
    }

    private void approve(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long productId = Long.valueOf(map.get("productId"));
        Long adminId = (Long) req.getAttribute("userId");

        auditService.approve(productId, adminId);
        writeJson(resp, Result.ok());
    }

    private void reject(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long productId = Long.valueOf(map.get("productId"));
        String reason = map.get("reason");
        Long adminId = (Long) req.getAttribute("userId");

        auditService.reject(productId, adminId, reason);
        writeJson(resp, Result.ok());
    }

    private void logs(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long productId = Long.valueOf(req.getParameter("productId"));
        writeJson(resp, Result.ok(auditService.findLogsByProductId(productId)));
    }
}