package com.klei.user.entity;

import com.klei.user.entity.enums.UserStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Integer vipLevel;
    private LocalDateTime vipExpireTime;
    private LocalDateTime banEndTime;
    private UserStatus status;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}