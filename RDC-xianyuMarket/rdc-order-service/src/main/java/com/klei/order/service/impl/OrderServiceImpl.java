package com.klei.order.service.impl;

import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.BusinessException;
import com.klei.common.mq.MqSender;
import com.klei.common.utils.RedisUtil;
import com.klei.order.dto.OrderCreateDTO;
import com.klei.order.entity.Order;
import com.klei.order.entity.Wallet;
import com.klei.order.entity.enums.OrderStatus;
import com.klei.order.entity.enums.WalletRecordType;
import com.klei.order.mapper.OrderMapper;
import com.klei.order.mapper.WalletMapper;
import com.klei.order.mapper.WalletRecordMapper;
import com.klei.order.service.OrderService;
import com.klei.product.entity.Product;
import com.klei.product.entity.enums.ProductStatus;
import com.klei.product.mapper.ProductMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private WalletMapper walletMapper;
    @Autowired
    private WalletRecordMapper walletRecordMapper;

    @Override
    @Transactional
    public String createOrder(Long buyerId, OrderCreateDTO dto) {
        String lockKey = "lock:product:" + dto.getProductId();
        String lockValue = buyerId + ":" + System.currentTimeMillis();
        boolean locked = RedisUtil.setnxex(lockKey, lockValue, 10);
        if (!locked) {
            throw new BusinessException("商品正在被其他用户购买，请稍后再试");
        }

        try {
            Product product = productMapper.findById(dto.getProductId());
            if (product == null || product.getIsDeleted() == 1) {
                throw new BusinessException("商品不存在");
            }
            if (!ProductStatus.PUBLISHED.equals(product.getStatus())) {
                throw new BusinessException("商品已下架或已售出");
            }
            if (product.getStock() <= 0) {
                throw new BusinessException("商品库存不足");
            }
            if (product.getUserId().equals(buyerId)) {
                throw new BusinessException("不能购买自己的商品");
            }

            Wallet buyerWallet = walletMapper.findByUserId(buyerId);
            if (buyerWallet == null) {
                throw new BusinessException("请先充值");
            }
            if (buyerWallet.getBalance().compareTo(product.getPrice()) < 0) {
                throw new BusinessException("余额不足");
            }

            int rows = productMapper.decrementStock(product.getId(), product.getVersion());
            if (rows == 0) {
                throw new BusinessException("商品已被他人购买");
            }

            // 扣买家钱并记账
            BigDecimal buyerNewBalance = buyerWallet.getBalance().subtract(product.getPrice());
            walletMapper.updateBalance(buyerNewBalance, buyerWallet.getId());
            walletRecordMapper.insert(buyerId, WalletRecordType.PAYMENT, product.getPrice(), buyerNewBalance, "购买商品支出");

            // 给卖家加钱并记账
            Wallet sellerWallet = walletMapper.findByUserId(product.getUserId());
            if (sellerWallet == null) {
                walletMapper.insert(product.getUserId());
                sellerWallet = walletMapper.findByUserId(product.getUserId());
            }
            BigDecimal sellerNewBalance = sellerWallet.getBalance().add(product.getPrice());
            walletMapper.updateBalance(sellerNewBalance, sellerWallet.getId());
            walletRecordMapper.insert(product.getUserId(), WalletRecordType.PAYMENT, product.getPrice(), sellerNewBalance, "商品销售收入");

            String orderNo = "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            orderMapper.insert(orderNo, buyerId, product.getUserId(), product.getId(), product.getPrice(), OrderStatus.PAID);
            productMapper.updateStatus(ProductStatus.SOLD, null, product.getId());

            // 改为 MQ 异步发送消息
            MqSender.sendMessage(product.getUserId(), "PRODUCT_SOLD",
                    "您的商品【" + product.getTitle() + "】已被购买，订单号：" + orderNo);

            return orderNo;
        } finally {
            // 修复：先比对 value，防止误删他人锁
            String currentValue = RedisUtil.get(lockKey);
            if (lockValue.equals(currentValue)) {
                RedisUtil.del(lockKey);
            }
        }
    }

    @Override
    @Transactional
    public void cancelOrder(Long buyerId, Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null || order.getIsDeleted() == 1) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (!OrderStatus.PAID.equals(order.getStatus())) {
            throw new BusinessException("订单状态不允许取消");
        }

        // 退买家钱
        Wallet buyerWallet = walletMapper.findByUserId(buyerId);
        BigDecimal buyerNewBalance = buyerWallet.getBalance().add(order.getPrice());
        walletMapper.updateBalance(buyerNewBalance, buyerWallet.getId());
        walletRecordMapper.insert(buyerId, WalletRecordType.REFUND, order.getPrice(), buyerNewBalance, "订单取消退款");

        // 扣卖家钱
        Wallet sellerWallet = walletMapper.findByUserId(order.getSellerId());
        if (sellerWallet != null) {
            BigDecimal sellerNewBalance = sellerWallet.getBalance().subtract(order.getPrice());
            walletMapper.updateBalance(sellerNewBalance, sellerWallet.getId());
            walletRecordMapper.insert(order.getSellerId(), WalletRecordType.PAYMENT, order.getPrice(), sellerNewBalance, "订单取消扣回");
        }

        // 恢复库存
        productMapper.incrementStock(order.getProductId());

        orderMapper.updateStatus(OrderStatus.CANCELLED, orderId);
        productMapper.updateStatus(ProductStatus.PUBLISHED, null, order.getProductId());
    }

    @Override
    @Transactional
    public void completeOrder(Long buyerId, Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null || order.getIsDeleted() == 1) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (!OrderStatus.PAID.equals(order.getStatus())) {
            throw new BusinessException("订单状态不允许确认收货");
        }

        orderMapper.updateStatus(OrderStatus.COMPLETED, orderId);
    }

    @Override
    public List<Order> findMyOrders(Long buyerId) {
        return orderMapper.findByBuyerId(buyerId);
    }

    @Override
    public List<Order> findMySells(Long sellerId) {
        return orderMapper.findBySellerId(sellerId);
    }
}