package com.klei.order.servlet;

import com.google.gson.reflect.TypeToken;
import com.klei.common.ioc.IoCContainer;
import com.klei.common.servlet.BaseServlet;
import com.klei.common.utils.AuthUtil;
import com.klei.common.utils.Result;
import com.klei.order.dto.OrderCreateDTO;
import com.klei.order.service.OrderService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

@WebServlet(urlPatterns = {"/order/*"})
public class OrderServlet extends BaseServlet {

    private OrderService orderService;
    private final Type mapType = new TypeToken<Map<String, String>>() {}.getType();

    @Override
    public void init() throws ServletException {
        super.init();
        this.orderService = IoCContainer.getBean(OrderService.class);
    }

    private void create(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long buyerId = AuthUtil.getUserId(req);
        OrderCreateDTO dto = gson.fromJson(req.getReader(), OrderCreateDTO.class);
        String orderNo = orderService.createOrder(buyerId, dto);
        writeJson(resp, Result.ok(orderNo));
    }

    private void cancel(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long buyerId = AuthUtil.getUserId(req);
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long orderId = Long.valueOf(map.get("orderId"));
        orderService.cancelOrder(buyerId, orderId);
        writeJson(resp, Result.ok());
    }

    private void complete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long buyerId = AuthUtil.getUserId(req);
        Map<String, String> map = gson.fromJson(req.getReader(), mapType);
        Long orderId = Long.valueOf(map.get("orderId"));
        orderService.completeOrder(buyerId, orderId);
        writeJson(resp, Result.ok());
    }

    private void myOrders(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long buyerId = AuthUtil.getUserId(req);
        writeJson(resp, Result.ok(orderService.findMyOrders(buyerId)));
    }

    private void mySells(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long sellerId = AuthUtil.getUserId(req);
        writeJson(resp, Result.ok(orderService.findMySells(sellerId)));
    }

    private void detail(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = AuthUtil.getUserId(req);
        Long orderId = Long.valueOf(req.getParameter("orderId"));
        com.klei.order.mapper.OrderMapper orderMapper = com.klei.common.ioc.IoCContainer.getBean(com.klei.order.mapper.OrderMapper.class);
        if (orderMapper == null) {
            orderMapper = com.klei.common.mapper.MapperProxyFactory.getMapper(com.klei.order.mapper.OrderMapper.class);
        }
        com.klei.order.entity.Order order = orderMapper.findById(orderId);
        if (order == null || order.getIsDeleted() == 1) {
            throw new com.klei.common.exception.BusinessException("订单不存在");
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new com.klei.common.exception.BusinessException("无权查看该订单");
        }
        writeJson(resp, Result.ok(order));
    }
}