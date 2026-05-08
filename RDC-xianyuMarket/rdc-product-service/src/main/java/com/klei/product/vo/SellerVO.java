package com.klei.product.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SellerVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer vipLevel;
    private LocalDateTime createdAt;
}
