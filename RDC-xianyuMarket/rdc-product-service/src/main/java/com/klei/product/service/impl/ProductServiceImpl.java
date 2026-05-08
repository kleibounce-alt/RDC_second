package com.klei.product.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.BusinessException;
import com.klei.common.mq.MqSender;
import com.klei.common.pool.ConnectionPool;
import com.klei.common.utils.GsonFactory;
import com.klei.common.utils.LogUtil;
import com.klei.common.utils.RedisUtil;
import com.klei.product.dto.ProductPublishDTO;
import com.klei.product.entity.Follow;
import com.klei.product.entity.Product;
import com.klei.product.entity.ProductImage;
import com.klei.product.entity.Tag;
import com.klei.product.entity.enums.ProductStatus;
import com.klei.product.mapper.*;
import com.klei.product.service.ProductService;
import com.klei.common.vo.PageResult;
import com.klei.product.vo.ProductDetailVO;
import com.klei.product.vo.SellerVO;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductImageMapper productImageMapper;
    @Autowired
    private ProductTagMapper productTagMapper;
    @Autowired
    private TagMapper tagMapper;
    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private FavoriteMapper favoriteMapper;

    private final Gson gson = GsonFactory.get();
    private static final int LIST_CACHE_SECONDS = 300;
    private static final Type PAGE_TYPE = new TypeToken<PageResult<Product>>() {}.getType();

    @Override
    @Transactional
    public long publish(Long userId, ProductPublishDTO dto) {
        long productId = productMapper.insert(
                userId,
                dto.getTitle(),
                dto.getDescription(),
                dto.getPrice(),
                dto.getStock() == null ? 1 : dto.getStock(),
                ProductStatus.PENDING
        );

        if (dto.getImages() != null) {
            int sort = 0;
            for (String url : dto.getImages()) {
                productImageMapper.insert(productId, url, sort++);
            }
        }

        if (dto.getTags() != null) {
            for (String tagName : dto.getTags()) {
                bindTag(productId, tagName);
            }
        }

        List<Follow> fans = followMapper.findByFollowUserId(userId);
        if (fans != null) {
            for (Follow fan : fans) {
                MqSender.sendMessage(fan.getUserId(), "FOLLOW_NEW_PRODUCT",
                        "您关注的人发布了新商品【" + dto.getTitle() + "】");
            }
        }

        List<Long> adminIds = loadAdminUserIds();
        if (adminIds != null) {
            for (Long adminId : adminIds) {
                MqSender.sendMessage(adminId, "NEW_PENDING_PRODUCT",
                        "有新的商品【" + dto.getTitle() + "】待审核，商品ID：" + productId);
            }
        }

        return productId;
    }

    @Override
    @Transactional
    public void edit(Long userId, Long productId, ProductPublishDTO dto) {
        Product product = productMapper.findById(productId);
        if (product == null || product.getIsDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }
        if (!product.getUserId().equals(userId)) {
            throw new BusinessException("无权编辑该商品");
        }

        productMapper.updateBasic(dto.getTitle(), dto.getDescription(), dto.getPrice(), dto.getStock(), productId);

        productImageMapper.deleteByProductId(productId);
        if (dto.getImages() != null) {
            int sort = 0;
            for (String url : dto.getImages()) {
                productImageMapper.insert(productId, url, sort++);
            }
        }

        productTagMapper.deleteByProductId(productId);
        if (dto.getTags() != null) {
            for (String tagName : dto.getTags()) {
                bindTag(productId, tagName);
            }
        }

        if (!ProductStatus.SOLD.equals(product.getStatus())) {
            productMapper.updateStatus(ProductStatus.PENDING, null, productId);
        }
    }

    @Override
    @Transactional
    public void offShelf(Long userId, Long productId) {
        Product product = productMapper.findById(productId);
        if (product == null || product.getIsDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }
        if (!product.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该商品");
        }
        productMapper.forceOffShelf("用户自行下架", productId);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long productId) {
        Product product = productMapper.findById(productId);
        if (product == null || product.getIsDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }
        if (!product.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该商品");
        }

        productMapper.deleteById(productId);
        productImageMapper.deleteByProductId(productId);
        productTagMapper.deleteByProductId(productId);
    }

    @Override
    public List<Product> myProducts(Long userId) {
        return productMapper.findByUserId(userId);
    }

    @Override
    public ProductDetailVO detail(Long productId, Long userId, String ip) {
        String cacheKey = "product:detail:" + productId;

        boolean needInvalidate = false;
        String dedupKey;
        if (userId != null) {
            dedupKey = "product:view:" + productId + ":u:" + userId;
        } else {
            dedupKey = "product:view:" + productId + ":ip:" + (ip != null ? ip : "unknown");
        }
        if (!RedisUtil.exists(dedupKey)) {
            productMapper.incrementViewCount(productId);
            RedisUtil.setnxex(dedupKey, "1", 24 * 60 * 60);
            needInvalidate = true;
        }

        if (needInvalidate) {
            RedisUtil.del(cacheKey);
            // 每10次浏览清一次列表缓存，避免首页浏览量长期不变
            Product p = productMapper.findById(productId);
            if (p != null && p.getViewCount() != null && p.getViewCount() % 10 == 0) {
                clearListCache();
            }
        } else {
            String cached = RedisUtil.hget(cacheKey, "data");
            if (cached != null) {
                ProductDetailVO vo = gson.fromJson(cached, ProductDetailVO.class);
                if (userId != null) {
                    vo.setIsFavorited(favoriteMapper.findByUserIdAndProductId(userId, productId) != null);
                }
                return vo;
            }
        }

        Product product = productMapper.findById(productId);
        if (product == null || product.getIsDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }

        ProductDetailVO vo = new ProductDetailVO();
        vo.setProduct(product);
        vo.setImages(productImageMapper.findByProductId(productId));
        vo.setTags(productTagMapper.findTagsByProductId(productId));
        vo.setSeller(querySeller(product.getUserId()));

        // isFavorited 不参与缓存，因为它是用户相关的
        RedisUtil.hset(cacheKey, "data", gson.toJson(vo));

        if (userId != null) {
            vo.setIsFavorited(favoriteMapper.findByUserIdAndProductId(userId, productId) != null);
        }
        return vo;
    }

    private SellerVO querySeller(Long userId) {
        String sql = "SELECT id, username, nickname, avatar, vip_level, created_at FROM sys_user WHERE id = ? AND is_deleted = 0";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SellerVO vo = new SellerVO();
                    vo.setId(rs.getLong("id"));
                    vo.setUsername(rs.getString("username"));
                    vo.setNickname(rs.getString("nickname"));
                    vo.setAvatar(rs.getString("avatar"));
                    vo.setVipLevel(rs.getInt("vip_level"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        vo.setCreatedAt(ts.toLocalDateTime());
                    }
                    return vo;
                }
            }
        } catch (SQLException e) {
            // 查询卖家失败不影响商品详情展示
        }
        return null;
    }

    @Override
    public PageResult<Product> list(int page, int size, Long tagId, Long userId) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }
        int offset = (page - 1) * size;

        boolean canCache = tagId == null && page <= 3;
        String cacheKey = "product:list:page:" + page + ":size:" + size;

        if (canCache) {
            String cached = RedisUtil.get(cacheKey);
            if (cached != null) {
                PageResult<Product> result = gson.fromJson(cached, PAGE_TYPE);
                applyFavoriteStatus(result, userId);
                return result;
            }
        }

        PageResult<Product> result = new PageResult<>();
        result.setPage(page);
        result.setSize(size);

        List<Product> list;
        long total;
        if (tagId != null) {
            list = productMapper.findByTagPage(tagId, offset, size);
            total = productMapper.countByTag(tagId);
        } else {
            list = productMapper.findPublishedPage(offset, size);
            total = productMapper.countPublished();
        }

        result.setList(list);
        result.setTotal(total);

        for (Product p : result.getList()) {
            List<ProductImage> images = productImageMapper.findByProductId(p.getId());
            p.setImages(images != null ? images : List.of());
        }

        if (canCache) {
            RedisUtil.setex(cacheKey, LIST_CACHE_SECONDS, gson.toJson(result));
        }

        applyFavoriteStatus(result, userId);
        return result;
    }

    private void applyFavoriteStatus(PageResult<Product> result, Long userId) {
        if (userId == null || result.getList() == null) {
            return;
        }
        Set<Long> favIds = new HashSet<>(favoriteMapper.findProductIdsByUserId(userId));
        for (Product p : result.getList()) {
            p.setIsFavorited(favIds.contains(p.getId()));
        }
    }

    @Override
    public PageResult<Product> search(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            PageResult<Product> empty = new PageResult<>();
            empty.setList(List.of());
            empty.setTotal(0);
            empty.setPage(page);
            empty.setSize(size);
            return empty;
        }
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }
        int offset = (page - 1) * size;
        String pattern = "%" + keyword.trim() + "%";
        List<Product> list = productMapper.searchPage(pattern, pattern, offset, size);
        long total = productMapper.countSearch(pattern, pattern);
        for (Product p : list) {
            List<ProductImage> images = productImageMapper.findByProductId(p.getId());
            p.setImages(images != null ? images : List.of());
        }
        PageResult<Product> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        return result;
    }

    @Override
    public List<Product> findUserPublishedProducts(Long userId) {
        List<Product> list = productMapper.findPublishedByUserId(userId);
        for (Product p : list) {
            List<ProductImage> images = productImageMapper.findByProductId(p.getId());
            p.setImages(images != null ? images : List.of());
        }
        return list;
    }

    private void bindTag(long productId, String tagName) {
        String name = tagName.trim();
        if (name.isEmpty()) {
            return;
        }
        Tag tag = tagMapper.findByName(name);
        Long tagId;
        if (tag == null) {
            tagId = tagMapper.insert(name);
        } else {
            tagId = tag.getId();
        }
        productTagMapper.insert(productId, tagId);
    }

    private List<Long> loadAdminUserIds() {
        String sql = "SELECT ur.user_id FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id WHERE r.code = 'ROLE_ADMIN' AND ur.is_deleted = 0 AND r.is_deleted = 0";
        List<Long> ids = new java.util.ArrayList<>();
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getLong("user_id"));
            }
        } catch (SQLException e) {
            LogUtil.error("查询管理员ID失败", e);
        }
        LogUtil.info("管理员通知: 找到 " + ids.size() + " 个管理员");
        return ids;
    }

    @Override
    public void clearListCache() {
        for (int i = 1; i <= 3; i++) {
            for (String key : RedisUtil.keys("product:list:page:" + i + ":size:*")) {
                RedisUtil.del(key);
            }
        }
    }
}