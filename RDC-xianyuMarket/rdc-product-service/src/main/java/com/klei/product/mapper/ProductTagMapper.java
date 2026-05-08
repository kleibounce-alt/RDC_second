package com.klei.product.mapper;

import com.klei.common.annotation.*;
import com.klei.product.entity.Tag;
import java.util.List;

public interface ProductTagMapper {

    @Select("SELECT t.* FROM tag t JOIN product_tag pt ON t.id = pt.tag_id WHERE pt.product_id = ? AND t.is_deleted = 0 AND pt.is_deleted = 0")
    List<Tag> findTagsByProductId(Long productId);

    @Insert("INSERT INTO product_tag (product_id, tag_id, is_deleted, created_at) VALUES (?, ?, 0, NOW())")
    int insert(Long productId, Long tagId);

    @Delete("DELETE FROM product_tag WHERE product_id = ?")
    int deleteByProductId(Long productId);

    @Update("UPDATE product_tag SET is_deleted = 1 WHERE product_id = ? AND tag_id = ?")
    int deleteByProductIdAndTagId(Long productId, Long tagId);
}