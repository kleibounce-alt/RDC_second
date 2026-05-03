package com.klei.product.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FollowVO {
    private Long followUserId;
    private String nickname;
    private String avatar;
    private LocalDateTime createdAt;
}