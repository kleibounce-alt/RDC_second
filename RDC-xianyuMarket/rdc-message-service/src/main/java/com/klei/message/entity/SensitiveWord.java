package com.klei.message.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SensitiveWord {
    private Long id;
    private String word;
    private Integer isDeleted;
    private LocalDateTime createdAt;
}