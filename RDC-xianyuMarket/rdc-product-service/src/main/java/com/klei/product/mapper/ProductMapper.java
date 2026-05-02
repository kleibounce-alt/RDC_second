package com.klei.product.mapper;

import com.klei.common.annotation.*;
import com.klei.product.entity.Product;
import com.klei.product.entity.enums.ProductStatus;
import java.math.BigDecimal;
import java.util.List;

public interface ProductMapper {

    @Select("SELECT * FROM product WHERE id = ? AND is_deleted = 0")
    Product findById(Long id);

    @Select("SELECT * FROM product WHERE user_id = ? AND is_deleted = 0 ORDER BY created_at DESC")
    List<Product> findByUserId(Long userId);

    @Select("SELECT * FROM product WHERE status = ? AND is_deleted = 0 ORDER BY created_at DESC")
    List<Product> findByStatus(ProductStatus status);

    @Insert("INSERT INTO product (user_id, title, description, price, stock, status, version, view_count, exposure_weight, is_deleted, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 0, 0, 0, 0, NOW(), NOW())")
    long insert(Long userId, String title, String description, BigDecimal price, Integer stock, ProductStatus status);

    @Update("UPDATE product SET title = ?, description = ?, price = ?, stock = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int updateBasic(String title, String description, BigDecimal price, Integer stock, Long id);

    @Update("UPDATE product SET status = ?, reject_reason = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int updateStatus(ProductStatus status, String rejectReason, Long id);

    @Update("UPDATE product SET view_count = view_count + 1, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int incrementViewCount(Long id);

    @Update("UPDATE product SET exposure_weight = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int updateExposureWeight(Integer exposureWeight, Long id);

    @Update("UPDATE product SET stock = stock - 1, version = version + 1, updated_at = NOW() WHERE id = ? AND stock > 0 AND is_deleted = 0")
    int decrementStock(Long id);

    @Update("UPDATE product SET is_deleted = 1, updated_at = NOW() WHERE id = ?")
    int deleteById(Long id);
}