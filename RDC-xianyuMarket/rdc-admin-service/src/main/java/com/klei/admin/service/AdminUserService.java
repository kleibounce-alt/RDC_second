package com.klei.admin.service;

import com.klei.product.entity.Product;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminUserService {

    void banUserByUsername(String username, LocalDateTime banEndTime);

    void unbanUserByUsername(String username);

    void relist(Long productId);

    void forceOffShelf(Long productId);

    void deleteComment(Long commentId);

    List<Product> findOffShelvedProducts();
}