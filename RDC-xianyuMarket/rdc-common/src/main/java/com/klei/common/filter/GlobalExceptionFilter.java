package com.klei.common.filter;

import com.google.gson.Gson;
import com.klei.xianyuMarket.rdc_common.exception.AuthException;
import com.klei.xianyuMarket.rdc_common.exception.BusinessException;
import com.klei.xianyuMarket.rdc_common.utils.LogUtil;
import com.klei.xianyuMarket.rdc_common.utils.Result;

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

        // 设置响应头，后面所有异常返回都是 JSON
        response.setContentType("application/json;charset=UTF-8");

        try {
            // 放行，让请求继续走后面的 Servlet
            chain.doFilter(req, resp);

        } catch (BusinessException e) {
            // 业务错误：库存不足、参数非法等，返回 500 + 错误信息
            LogUtil.warn("业务异常: " + e.getMessage() + " | URI=" + request.getRequestURI());
            String json = gson.toJson(Result.fail(e.getMessage()));
            // HTTP 状态码保持 200，业务错误走 code=500
            response.setStatus(200);
            response.getWriter().write(json);

        } catch (AuthException e) {
            // 鉴权错误：未登录、无权限
            LogUtil.warn("鉴权异常: " + e.getMessage() + " | URI=" + request.getRequestURI());
            String json = gson.toJson(Result.fail(e.getCode(), e.getMessage()));
            // 401 或 403
            response.setStatus(e.getCode());
            response.getWriter().write(json);

        } catch (Exception e) {
            // 系统错误：数据库挂了、空指针等，返回 500 + 统一话术
            LogUtil.error("系统异常: " + e.getMessage() + " | URI=" + request.getRequestURI(), e);
            String json = gson.toJson(Result.fail("系统繁忙，请稍后再试"));
            response.setStatus(500);
            response.getWriter().write(json);
        }
    }

    @Override
    public void destroy() {}
}