package com.klei.common.filter;

import com.google.gson.Gson;
import com.klei.common.exception.AuthException;
import com.klei.common.exception.BusinessException;
import com.klei.common.utils.LogUtil;
import com.klei.common.utils.Result;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/*")
public class GlobalExceptionFilter implements Filter {

    private final Gson gson = new Gson();

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        response.setContentType("application/json;charset=UTF-8");

        try {
            chain.doFilter(req, resp);

        } catch (BusinessException e) {
            LogUtil.warn("业务异常: " + e.getMessage() + " | URI=" + request.getRequestURI());
            String json = gson.toJson(Result.fail(e.getMessage()));
            response.setStatus(200);
            response.getWriter().write(json);

        } catch (AuthException e) {
            LogUtil.warn("鉴权异常: " + e.getMessage() + " | URI=" + request.getRequestURI());
            String json = gson.toJson(Result.fail(e.getCode(), e.getMessage()));
            response.setStatus(e.getCode());
            response.getWriter().write(json);

        } catch (Exception e) {
            LogUtil.error("系统异常: " + e.getMessage() + " | URI=" + request.getRequestURI(), e);
            String json = gson.toJson(Result.fail("系统繁忙，请稍后再试"));
            response.setStatus(500);
            response.getWriter().write(json);
        }
    }

    @Override
    public void destroy() {}
}