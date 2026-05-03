package com.klei.order.servlet;

import com.google.gson.reflect.TypeToken;
import com.klei.common.ioc.IoCContainer;
import com.klei.common.servlet.BaseServlet;
import com.klei.common.utils.AuthUtil;
import com.klei.common.utils.Result;
import com.klei.order.service.WalletService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Map;

@WebServlet(urlPatterns = {"/wallet/*"})
public class WalletServlet extends BaseServlet {

    private WalletService walletService;
    private final Type mapType = new TypeToken<Map<String, String>>() {}.getType();

    @Override
    public void init() throws ServletException {
        super.init();
        this.walletService = IoCContainer.getBean(WalletService.class);
    }

    private void recharge(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        BigDecimal amount = new BigDecimal(map.get("amount"));
        walletService.recharge(userId, amount);
        writeJson(resp, Result.ok());
    }

    private void balance(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        writeJson(resp, Result.ok(walletService.getBalance(userId)));
    }

    private void records(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        writeJson(resp, Result.ok(walletService.getRecords(userId)));
    }
}