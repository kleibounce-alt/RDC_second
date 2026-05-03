package com.klei.product.servlet;

import com.google.gson.reflect.TypeToken;
import com.klei.common.ioc.IoCContainer;
import com.klei.common.servlet.BaseServlet;
import com.klei.common.utils.AuthUtil;
import com.klei.common.utils.FileUploadUtil;
import com.klei.common.utils.Result;
import com.klei.product.dto.ProductPublishDTO;
import com.klei.product.service.ProductService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {
        "/publish", "/edit", "/delete",
        "/my-products", "/detail", "/list", "/search",
        "/upload"
})
public class ProductServlet extends BaseServlet {

    private ProductService productService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.productService = IoCContainer.getBean(ProductService.class);
    }

    private void publish(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        ProductPublishDTO dto = gson.fromJson(req.getReader(), ProductPublishDTO.class);
        long productId = productService.publish(userId, dto);
        writeJson(resp, Result.ok(productId));
    }

    private void edit(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        Map<String, Object> map = gson.fromJson(req.getReader(), new TypeToken<Map<String, Object>>() {}.getType());
        Long productId = ((Number) map.remove("productId")).longValue();
        ProductPublishDTO dto = gson.fromJson(gson.toJson(map), ProductPublishDTO.class);
        productService.edit(userId, productId, dto);
        writeJson(resp, Result.ok());
    }

    private void delete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        Map<String, String> map = gson.fromJson(req.getReader(), new TypeToken<Map<String, String>>() {}.getType());
        Long productId = Long.valueOf(map.get("productId"));
        productService.delete(userId, productId);
        writeJson(resp, Result.ok());
    }

    private void myProducts(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        writeJson(resp, Result.ok(productService.myProducts(userId)));
    }

    private void detail(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long productId = Long.valueOf(req.getParameter("productId"));
        Long userId = AuthUtil.getUserId(req);
        writeJson(resp, Result.ok(productService.detail(productId, userId)));
    }

    private void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int page = parseInt(req.getParameter("page"), 1);
        int size = parseInt(req.getParameter("size"), 10);
        Long tagId = req.getParameter("tagId") != null ? Long.valueOf(req.getParameter("tagId")) : null;
        writeJson(resp, Result.ok(productService.list(page, size, tagId)));
    }

    private void search(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String keyword = req.getParameter("keyword");
        writeJson(resp, Result.ok(productService.search(keyword)));
    }

    private void upload(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        List<String> paths = FileUploadUtil.uploadImages(req, "product", 10);
        writeJson(resp, Result.ok(paths));
    }

    private int parseInt(String val, int defaultVal) {
        if (val == null || val.isEmpty()) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}