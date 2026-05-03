package com.klei.product.service;

import com.klei.product.dto.ProductPublishDTO;
import com.klei.product.entity.Product;
import com.klei.product.vo.PageResult;
import com.klei.product.vo.ProductDetailVO;
import java.util.List;

public interface ProductService {

    long publish(Long userId, ProductPublishDTO dto);

    void edit(Long userId, Long productId, ProductPublishDTO dto);

    void delete(Long userId, Long productId);

    List<Product> myProducts(Long userId);

    ProductDetailVO detail(Long productId, Long userId);

    PageResult<Product> list(int page, int size, Long tagId);

    List<Product> search(String keyword);
}