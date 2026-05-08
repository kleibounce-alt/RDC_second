package com.klei.admin.service.impl;

import com.klei.admin.service.AdminProductService;
import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.BusinessException;
import com.klei.common.mq.MqSender;
import com.klei.product.entity.Product;
import com.klei.product.mapper.ProductMapper;

@Component
public class AdminProductServiceImpl implements AdminProductService {

    private static final int[] EXPOSURE_WEIGHTS = {0, 10, 30, 60, 100};

    @Autowired
    private ProductMapper productMapper;

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productMapper.findById(productId);
        if (product == null || product.getIsDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }
        productMapper.deleteById(productId);
        MqSender.sendMessage(product.getUserId(), "AUDIT_RESULT",
                "您的商品【" + product.getTitle() + "】已被管理员删除");
    }

    @Override
    @Transactional
    public void setExposure(Long productId, int level) {
        if (level < 1 || level > 5) {
            throw new BusinessException("曝光档位需在1-5之间");
        }
        Product product = productMapper.findById(productId);
        if (product == null || product.getIsDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }
        int weight = EXPOSURE_WEIGHTS[level - 1];
        productMapper.updateExposureWeight(weight, productId);
    }
}