package com.klei.product.vo;

import com.klei.product.entity.Product;
import com.klei.product.entity.ProductImage;
import com.klei.product.entity.Tag;
import lombok.Data;
import java.util.List;

@Data
public class ProductDetailVO {
    private Product product;
    private List<ProductImage> images;
    private List<Tag> tags;
    private SellerVO seller;
    private Boolean isFavorited;
}