package com.klei.product.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.klei.common.annotation.Autowired;
import com.klei.common.annotation.Component;
import com.klei.common.annotation.Transactional;
import com.klei.common.exception.BusinessException;
import com.klei.common.mq.MqSender;
import com.klei.common.utils.RedisUtil;
import com.klei.product.dto.ProductPublishDTO;
import com.klei.product.entity.Follow;
import com.klei.product.entity.Product;
import com.klei.product.entity.ProductImage;
import com.klei.product.entity.Tag;
import com.klei.product.entity.enums.ProductStatus;
import com.klei.product.mapper.*;
import com.klei.product.service.ProductService;
import com.klei.product.vo.PageResult;
import com.klei.product.vo.ProductDetailVO;

import java.lang.reflect.Type;
import java.util.List;

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

    private final Gson gson = new Gson();
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
                // 改为 MQ 异步发送
                MqSender.sendMessage(fan.getUserId(), "FOLLOW_NEW_PRODUCT",
                        "您关注的人发布了新商品【" + dto.getTitle() + "】");
            }
        }

        // 发布新商品后清列表缓存
        clearListCache();

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

        RedisUtil.del("product:detail:" + productId);
        clearListCache();
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

        RedisUtil.del("product:detail:" + productId);
        clearListCache();
    }

    @Override
    public List<Product> myProducts(Long userId) {
        return productMapper.findByUserId(userId);
    }

    @Override
    public ProductDetailVO detail(Long productId, Long userId) {
        String cacheKey = "product:detail:" + productId;

        boolean needInvalidate = false;
        if (userId != null) {
            String viewKey = "product:view:" + productId + ":" + userId;
            if (!RedisUtil.exists(viewKey)) {
                productMapper.incrementViewCount(productId);
                RedisUtil.setnxex(viewKey, "1", 24 * 60 * 60);
                needInvalidate = true;
            }
        } else {
            productMapper.incrementViewCount(productId);
            needInvalidate = true;
        }

        if (needInvalidate) {
            RedisUtil.del(cacheKey);
        } else {
            String cached = RedisUtil.hget(cacheKey, "data");
            if (cached != null) {
                return gson.fromJson(cached, ProductDetailVO.class);
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

        RedisUtil.hset(cacheKey, "data", gson.toJson(vo));
        return vo;
    }

    @Override
    public PageResult<Product> list(int page, int size, Long tagId) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }
        int offset = (page - 1) * size;

        // 热点列表缓存：首页前3页 + 无标签筛选才缓存
        boolean canCache = tagId == null && page <= 3;
        String cacheKey = "product:list:page:" + page + ":size:" + size;

        if (canCache) {
            String cached = RedisUtil.get(cacheKey);
            if (cached != null) {
                return gson.fromJson(cached, PAGE_TYPE);
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

        if (canCache) {
            RedisUtil.setex(cacheKey, LIST_CACHE_SECONDS, gson.toJson(result));
        }
        return result;
    }

    @Override
    public List<Product> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String pattern = "%" + keyword.trim() + "%";
        return productMapper.search(pattern, pattern);
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

    private void clearListCache() {
        // 简单做法：删除前3页缓存
        for (int i = 1; i <= 3; i++) {
            for (int s : new int[]{10, 20}) {
                RedisUtil.del("product:list:page:" + i + ":size:" + s);
            }
        }
    }
}