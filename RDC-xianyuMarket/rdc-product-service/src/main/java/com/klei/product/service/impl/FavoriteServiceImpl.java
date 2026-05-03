package com.klei.product.service.impl;

import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.BusinessException;
import com.klei.product.entity.Favorite;
import com.klei.product.entity.Product;
import com.klei.product.entity.ProductImage;
import com.klei.product.mapper.FavoriteMapper;
import com.klei.product.mapper.ProductImageMapper;
import com.klei.product.mapper.ProductMapper;
import com.klei.product.service.FavoriteService;
import com.klei.product.vo.FavoriteVO;

import java.util.ArrayList;
import java.util.List;

@Component
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductImageMapper productImageMapper;

    @Override
    @Transactional
    public void toggleFavorite(Long userId, Long productId) {
        Product product = productMapper.findById(productId);
        if (product == null || product.getIsDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }

        Favorite fav = favoriteMapper.findByUserIdAndProductId(userId, productId);
        if (fav == null || fav.getIsDeleted() == 1) {
            favoriteMapper.insert(userId, productId);
        } else {
            favoriteMapper.deleteByUserIdAndProductId(userId, productId);
        }
    }

    @Override
    public List<FavoriteVO> findMyFavorites(Long userId) {
        List<Favorite> list = favoriteMapper.findByUserId(userId);
        List<FavoriteVO> result = new ArrayList<>();

        for (Favorite f : list) {
            Product p = productMapper.findById(f.getProductId());
            if (p == null || p.getIsDeleted() == 1) {
                continue;
            }

            FavoriteVO vo = new FavoriteVO();
            vo.setProductId(f.getProductId());
            vo.setProductTitle(p.getTitle());
            vo.setProductPrice(p.getPrice());

            List<ProductImage> imgs = productImageMapper.findByProductId(p.getId());
            vo.setProductImage(imgs.isEmpty() ? null : imgs.get(0).getImageUrl());
            vo.setCreatedAt(f.getCreatedAt());

            result.add(vo);
        }
        return result;
    }
}