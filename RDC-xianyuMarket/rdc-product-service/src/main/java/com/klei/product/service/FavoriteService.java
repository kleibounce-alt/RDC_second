package com.klei.product.service;

import com.klei.common.vo.PageResult;
import com.klei.product.vo.FavoriteVO;

public interface FavoriteService {

    void toggleFavorite(Long userId, Long productId);

    PageResult<FavoriteVO> findMyFavorites(Long userId, int page, int size);
}