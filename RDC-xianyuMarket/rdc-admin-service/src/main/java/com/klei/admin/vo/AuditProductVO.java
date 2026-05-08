package com.klei.admin.vo;

import com.klei.product.entity.Product;
import com.klei.user.entity.User;
import lombok.Data;

@Data
public class AuditProductVO {
    private Product product;
    private User seller;
}
