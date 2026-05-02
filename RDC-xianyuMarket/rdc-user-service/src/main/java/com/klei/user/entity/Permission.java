package com.klei.user.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Permission {
    private Long id;
    private String name;
    private String code;
    private String urlPattern;
    private Integer isDeleted;
    private LocalDateTime createdAt;
}