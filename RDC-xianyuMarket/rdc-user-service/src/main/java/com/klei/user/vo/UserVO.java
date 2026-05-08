package com.klei.user.vo;

import com.klei.user.entity.enums.UserStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Integer vipLevel;
    private LocalDateTime vipExpireTime;
    private UserStatus status;
    private LocalDateTime banEndTime;
    private List<String> roles;
    private LocalDateTime createdAt;
}