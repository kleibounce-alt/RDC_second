package com.klei.admin.service.impl;

import com.klei.admin.service.AuditService;
import com.klei.admin.vo.AuditProductVO;
import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.BusinessException;
import com.klei.common.mq.MqSender;
import com.klei.admin.entity.AuditLog;
import com.klei.admin.entity.enums.AuditAction;
import com.klei.admin.mapper.AuditLogMapper;
import com.klei.product.entity.Product;
import com.klei.product.entity.enums.ProductStatus;
import com.klei.product.mapper.ProductMapper;
import com.klei.user.entity.User;
import com.klei.user.mapper.UserMapper;

import java.util.ArrayList;
import java.util.List;

@Component
public class AuditServiceImpl implements AuditService {

    @Autowired
    private AuditLogMapper auditLogMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public List<AuditProductVO> findPendingProducts() {
        List<Product> products = productMapper.findByStatus(ProductStatus.PENDING);
        List<AuditProductVO> result = new ArrayList<>();
        for (Product p : products) {
            AuditProductVO vo = new AuditProductVO();
            vo.setProduct(p);
            User seller = userMapper.findById(p.getUserId());
            if (seller != null) {
                seller.setPassword(null); // 不暴露密码
            }
            vo.setSeller(seller);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public void approve(Long productId, Long adminId) {
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!ProductStatus.PENDING.equals(product.getStatus())) {
            throw new BusinessException("该商品不在待审核状态");
        }

        int rows = productMapper.updateStatus(ProductStatus.PUBLISHED, null, productId);
        if (rows == 0) {
            throw new BusinessException("该商品已被其他管理员处理");
        }

        auditLogMapper.insert(productId, adminId, AuditAction.APPROVE, null);

        MqSender.sendMessage(product.getUserId(), "AUDIT_RESULT",
                "您的商品【" + product.getTitle() + "】已通过审核并发布");
    }

    @Override
    @Transactional
    public void reject(Long productId, Long adminId, String reason) {
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!ProductStatus.PENDING.equals(product.getStatus())) {
            throw new BusinessException("该商品不在待审核状态");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("驳回原因不能为空");
        }

        int rows = productMapper.updateStatus(ProductStatus.REJECTED, reason, productId);
        if (rows == 0) {
            throw new BusinessException("该商品已被其他管理员处理");
        }

        auditLogMapper.insert(productId, adminId, AuditAction.REJECT, reason);

        MqSender.sendMessage(product.getUserId(), "AUDIT_RESULT",
                "您的商品【" + product.getTitle() + "】审核未通过，原因：" + reason);
    }

    @Override
    public List<AuditLog> findLogsByProductId(Long productId) {
        return auditLogMapper.findByProductId(productId);
    }
}