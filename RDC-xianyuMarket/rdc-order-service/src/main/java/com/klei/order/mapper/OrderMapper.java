package com.klei.order.mapper;

import com.klei.common.annotation.*;
import com.klei.order.entity.Order;
import com.klei.order.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.util.List;

public interface OrderMapper {

    @Select("SELECT * FROM orders WHERE id = ? AND is_deleted = 0")
    Order findById(Long id);

    @Select("SELECT * FROM orders WHERE buyer_id = ? AND is_deleted = 0 ORDER BY created_at DESC")
    List<Order> findByBuyerId(Long buyerId);

    @Select("SELECT * FROM orders WHERE seller_id = ? AND is_deleted = 0 ORDER BY created_at DESC")
    List<Order> findBySellerId(Long sellerId);

    @Select("SELECT * FROM orders WHERE order_no = ? AND is_deleted = 0")
    Order findByOrderNo(String orderNo);

    @Insert("INSERT INTO orders (order_no, buyer_id, seller_id, product_id, price, status, is_deleted, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 0, NOW(), NOW())")
    long insert(String orderNo, Long buyerId, Long sellerId, Long productId, BigDecimal price, OrderStatus status);

    @Update("UPDATE orders SET status = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int updateStatus(OrderStatus status, Long id);

    @Update("UPDATE orders SET is_deleted = 1, updated_at = NOW() WHERE id = ?")
    int deleteById(Long id);
}