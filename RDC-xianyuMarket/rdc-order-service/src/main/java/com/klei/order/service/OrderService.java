package com.klei.order.service;

import com.klei.order.dto.OrderCreateDTO;
import com.klei.order.entity.Order;
import java.util.List;

public interface OrderService {

    String createOrder(Long buyerId, OrderCreateDTO dto);

    void cancelOrder(Long buyerId, Long orderId);

    void requestRefund(Long buyerId, Long orderId);

    void handleRefund(Long sellerId, Long orderId, boolean approve);

    void completeOrder(Long buyerId, Long orderId);

    List<Order> findMyOrders(Long buyerId);

    List<Order> findMySells(Long sellerId);
}