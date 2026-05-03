package com.klei.product.servlet;

import com.google.gson.reflect.TypeToken;
import com.klei.common.ioc.IoCContainer;
import com.klei.common.servlet.BaseServlet;
import com.klei.common.utils.AuthUtil;
import com.klei.common.utils.Result;
import com.klei.product.service.FavoriteService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

@WebServlet(urlPatterns = {"/favorite/*"})
public class FavoriteServlet extends BaseServlet {

    private FavoriteService favoriteService;
    private final Type mapType = new TypeToken<Map<String, String>>() {}.getType();

    @Override
    public void init() throws ServletException {
        super.init();
        this.favoriteService = IoCContainer.getBean(FavoriteService.class);
    }

    private void toggle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long productId = Long.valueOf(map.get("productId"));
        favoriteService.toggleFavorite(userId, productId);
        writeJson(resp, Result.ok());
    }

    private void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        writeJson(resp, Result.ok(favoriteService.findMyFavorites(userId)));
    }
}