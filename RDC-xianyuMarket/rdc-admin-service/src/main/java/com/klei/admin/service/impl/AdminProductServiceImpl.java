package com.klei.admin.service.impl;

import com.klei.admin.service.AdminProductService;
import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.BusinessException;
import com.klei.product.entity.Product;
import com.klei.product.mapper.ProductMapper;

@Component
public class AdminProductServiceImpl implements AdminProductService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    @Transactional
    public void increaseExposure(Long productId, Integer weight) {
        if (weight == null || weight < 0) {
            throw new BusinessException("曝光权重不能为负数");
        }
        Product product = productMapper.findById(productId);
        if (product == null || product.getIsDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }
        productMapper.updateExposureWeight(weight, productId);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productMapper.findById(productId);
        if (product == null || product.getIsDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }
        productMapper.deleteById(productId);
    }
}