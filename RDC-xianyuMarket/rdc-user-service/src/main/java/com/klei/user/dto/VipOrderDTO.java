package com.klei.user.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VipOrderDTO {
    private Integer vipLevel;
    private Integer durationMonths;
    private BigDecimal price;
}