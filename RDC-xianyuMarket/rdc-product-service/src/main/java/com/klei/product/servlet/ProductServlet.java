package com.klei.product.servlet;

import com.google.gson.reflect.TypeToken;
import com.klei.common.ioc.IoCContainer;
import com.klei.common.servlet.BaseServlet;
import com.klei.common.utils.AuthUtil;
import com.klei.common.utils.FileUploadUtil;
import com.klei.common.utils.RedisUtil;
import com.klei.common.utils.Result;
import com.klei.product.dto.ProductPublishDTO;
import com.klei.product.mapper.ProductMapper;
import com.klei.product.mapper.TagMapper;
import com.klei.product.service.ProductService;
import com.klei.product.entity.enums.ProductStatus;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {
        "/publish", "/edit", "/delete", "/off-shelf",
        "/my-products", "/detail", "/list", "/search",
        "/upload", "/tag-list", "/tag-create", "/user-products"
})
public class ProductServlet extends BaseServlet {

    private ProductService productService;
    private TagMapper tagMapper;
    private ProductMapper productMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.productService = IoCContainer.getBean(ProductService.class);
        this.tagMapper = IoCContainer.getBean(TagMapper.class);
        this.productMapper = IoCContainer.getBean(ProductMapper.class);
    }

    @SuppressWarnings("unchecked")
    private void publish(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        ProductPublishDTO dto = gson.fromJson(req.getReader(), ProductPublishDTO.class);
        long productId = productService.publish(userId, dto);
        List<String> roles = (List<String>) req.getAttribute("roles");
        if (roles != null && roles.contains("ROLE_ADMIN")) {
            productMapper.updateStatus(ProductStatus.PUBLISHED, null, productId);
            clearProductCache(productId);
        }
        writeJson(resp, Result.ok(productId));
    }

    private void edit(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        ProductPublishDTO dto = gson.fromJson(req.getReader(), ProductPublishDTO.class);
        productService.edit(userId, dto.getProductId(), dto);
        clearProductCache(dto.getProductId());
        writeJson(resp, Result.ok());
    }

    private void offShelf(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        Map<String, String> map = gson.fromJson(req.getReader(), new TypeToken<Map<String, String>>() {}.getType());
        Long productId = Long.valueOf(map.get("productId"));
        productService.offShelf(userId, productId);
        clearProductCache(productId);
        writeJson(resp, Result.ok());
    }

    private void delete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        Map<String, String> map = gson.fromJson(req.getReader(), new TypeToken<Map<String, String>>() {}.getType());
        Long productId = Long.valueOf(map.get("productId"));
        productService.delete(userId, productId);
        clearProductCache(productId);
        writeJson(resp, Result.ok());
    }

    private void clearProductCache(Long productId) {
        RedisUtil.del("product:detail:" + productId);
        for (String key : RedisUtil.keys("product:list:*")) {
            RedisUtil.del(key);
        }
    }

    private void myProducts(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        writeJson(resp, Result.ok(productService.myProducts(userId)));
    }

    private void detail(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long productId = Long.valueOf(req.getParameter("productId"));
        Long userId = AuthUtil.getUserId(req);
        String ip = getClientIp(req);
        writeJson(resp, Result.ok(productService.detail(productId, userId, ip)));
    }

    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            ip = ip.split(",")[0].trim();
            return ip;
        }
        return req.getRemoteAddr();
    }

    private void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int page = parseInt(req.getParameter("page"), 1);
        int size = parseInt(req.getParameter("size"), 10);
        Long tagId = req.getParameter("tagId") != null ? Long.valueOf(req.getParameter("tagId")) : null;
        Long userId = AuthUtil.getUserId(req);
        writeJson(resp, Result.ok(productService.list(page, size, tagId, userId)));
    }

    private void search(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String keyword = req.getParameter("keyword");
        int page = parseInt(req.getParameter("page"), 1);
        int size = parseInt(req.getParameter("size"), 10);
        writeJson(resp, Result.ok(productService.search(keyword, page, size)));
    }

    private void userProducts(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = Long.valueOf(req.getParameter("userId"));
        writeJson(resp, Result.ok(productService.findUserPublishedProducts(userId)));
    }

    private void upload(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        List<String> paths = FileUploadUtil.uploadImages(req, "product", 10);
        List<String> fullPaths = new java.util.ArrayList<>();
        for (String p : paths) {
            fullPaths.add("/p/" + p);
        }
        writeJson(resp, Result.ok(fullPaths));
    }

    private void tagList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        writeJson(resp, Result.ok(tagMapper.findAll()));
    }

    private void tagCreate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> map = gson.fromJson(req.getReader(), new TypeToken<Map<String, String>>() {}.getType());
        String name = map.get("name");
        if (name == null || name.trim().isEmpty()) {
            writeJson(resp, Result.fail("标签名不能为空"));
            return;
        }
        name = name.trim();
        if (tagMapper.findByName(name) != null) {
            writeJson(resp, Result.fail("标签已存在"));
            return;
        }
        long tagId = tagMapper.insert(name);
        writeJson(resp, Result.ok(tagId));
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