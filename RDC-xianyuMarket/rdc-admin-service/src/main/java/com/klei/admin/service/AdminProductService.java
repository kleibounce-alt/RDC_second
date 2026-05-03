package com.klei.admin.service;

public interface AdminProductService {

    void increaseExposure(Long productId, Integer weight);

    void deleteProduct(Long productId);
}