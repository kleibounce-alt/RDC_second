package com.klei.admin.service;

import java.time.LocalDateTime;

public interface AdminUserService {

    void banUser(Long userId, LocalDateTime banEndTime);

    void unbanUser(Long userId);

    void forceOffShelf(Long productId);

    void deleteComment(Long commentId);
}