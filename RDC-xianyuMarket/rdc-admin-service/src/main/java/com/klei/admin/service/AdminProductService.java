package com.klei.admin.service;

public interface AdminProductService {

    void deleteProduct(Long productId);

    void setExposure(Long productId, int level);
}