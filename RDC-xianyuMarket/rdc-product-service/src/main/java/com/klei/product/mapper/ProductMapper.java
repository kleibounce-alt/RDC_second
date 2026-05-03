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

    @Update("UPDATE product SET status = ?, reject_reason = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0 AND status = 'PENDING'")
    int updateStatus(ProductStatus status, String rejectReason, Long id);

    @Update("UPDATE product SET view_count = view_count + 1, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int incrementViewCount(Long id);

    @Update("UPDATE product SET exposure_weight = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int updateExposureWeight(Integer exposureWeight, Long id);

    @Update("UPDATE product SET stock = stock - 1, version = version + 1, updated_at = NOW() WHERE id = ? AND stock > 0 AND version = ? AND is_deleted = 0")
    int decrementStock(Long id, Integer version);

    @Update("UPDATE product SET stock = stock + 1, version = version + 1, updated_at = NOW() WHERE id = ? AND is_deleted = 0")
    int incrementStock(Long id);

    @Update("UPDATE product SET is_deleted = 1, updated_at = NOW() WHERE id = ?")
    int deleteById(Long id);

    // 曝光抑制：view_count > 1000 后权重增长降为 1/10，防止热门商品永远霸榜
    @Select("SELECT p.* FROM product p LEFT JOIN sys_user u ON p.user_id = u.id WHERE p.status = 'PUBLISHED' AND p.is_deleted = 0 ORDER BY (p.exposure_weight * 10 + CASE WHEN p.view_count <= 1000 THEN p.view_count ELSE 1000 + (p.view_count - 1000) / 10 END + IFNULL(u.vip_level, 0) * 50) DESC LIMIT ?, ?")
    List<Product> findPublishedPage(int offset, int size);

    @Select("SELECT COUNT(*) FROM product WHERE status = 'PUBLISHED' AND is_deleted = 0")
    long countPublished();

    @Select("SELECT p.* FROM product p JOIN product_tag pt ON p.id = pt.product_id LEFT JOIN sys_user u ON p.user_id = u.id WHERE pt.tag_id = ? AND p.status = 'PUBLISHED' AND p.is_deleted = 0 AND pt.is_deleted = 0 ORDER BY (p.exposure_weight * 10 + CASE WHEN p.view_count <= 1000 THEN p.view_count ELSE 1000 + (p.view_count - 1000) / 10 END + IFNULL(u.vip_level, 0) * 50) DESC LIMIT ?, ?")
    List<Product> findByTagPage(Long tagId, int offset, int size);

    @Select("SELECT COUNT(*) FROM product p JOIN product_tag pt ON p.id = pt.product_id WHERE pt.tag_id = ? AND p.status = 'PUBLISHED' AND p.is_deleted = 0 AND pt.is_deleted = 0")
    long countByTag(Long tagId);

    @Select("SELECT * FROM product WHERE (title LIKE ? OR description LIKE ?) AND status = 'PUBLISHED' AND is_deleted = 0 ORDER BY created_at DESC")
    List<Product> search(String titlePattern, String descPattern);

    @Update("UPDATE product SET status = 'REJECTED', reject_reason = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0 AND status = 'PUBLISHED'")
    int forceOffShelf(String reason, Long id);
}