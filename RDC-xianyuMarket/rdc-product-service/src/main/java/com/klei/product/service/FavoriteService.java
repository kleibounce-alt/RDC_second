package com.klei.product.service;

import com.klei.product.vo.FavoriteVO;
import java.util.List;

public interface FavoriteService {

    void toggleFavorite(Long userId, Long productId);

    List<FavoriteVO> findMyFavorites(Long userId);
}