package com.klei.product.mapper;

import com.klei.common.annotation.*;
import com.klei.product.entity.ProductImage;
import java.util.List;

public interface ProductImageMapper {

    @Select("SELECT * FROM product_image WHERE id = ? AND is_deleted = 0")
    ProductImage findById(Long id);

    @Select("SELECT * FROM product_image WHERE product_id = ? AND is_deleted = 0 ORDER BY sort ASC")
    List<ProductImage> findByProductId(Long productId);

    @Insert("INSERT INTO product_image (product_id, image_url, sort, is_deleted, created_at) VALUES (?, ?, ?, 0, NOW())")
    long insert(Long productId, String imageUrl, Integer sort);

    @Update("UPDATE product_image SET is_deleted = 1 WHERE id = ?")
    int deleteById(Long id);

    @Update("UPDATE product_image SET is_deleted = 1 WHERE product_id = ?")
    int deleteByProductId(Long productId);
}