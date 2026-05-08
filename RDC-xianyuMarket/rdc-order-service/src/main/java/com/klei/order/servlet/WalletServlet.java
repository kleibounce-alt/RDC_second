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
        String amountStr = map.get("amount");
        if (amountStr == null || amountStr.isEmpty()) {
            writeJson(resp, Result.fail("请输入充值金额"));
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
        } catch (NumberFormatException e) {
            writeJson(resp, Result.fail("充值金额格式不正确"));
            return;
        }
        if (amount.compareTo(new BigDecimal("99999999.99")) > 0) {
            writeJson(resp, Result.fail("单次充值不能超过99,999,999.99元"));
            return;
        }
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