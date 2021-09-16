/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.common.utils.NormalizeUtil;
import io.geekstore.entity.*;
import io.geekstore.eventbus.events.ProductEvent;
import io.geekstore.exception.UserInputException;
import io.geekstore.mapper.*;
import io.geekstore.service.helpers.PageInfo;
import io.geekstore.service.helpers.QueryHelper;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.DeletionResult;
import io.geekstore.types.product.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.eventbus.EventBus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class ProductService {

    private final ProductEntityMapper productEntityMapper;
    private final ProductFacetValueJoinEntityMapper productFacetValueJoinEntityMapper;
    private final FacetValueEntityMapper facetValueEntityMapper;
    private final AssetEntityMapper assetEntityMapper;
    private final ProductAssetJoinEntityMapper productAssetJoinEntityMapper;
    private final ProductOptionGroupEntityMapper productOptionGroupEntityMapper;
    private final ProductOptionGroupJoinEntityMapper productOptionGroupJoinEntityMapper;
    private final ProductVariantEntityMapper productVariantEntityMapper;

    private final EventBus eventBus;

    public ProductList findAll(ProductListOptions options) {
        PageInfo pageInfo = ServiceHelper.getListOptions(options);
        IPage<ProductEntity> page = new Page<>(pageInfo.current, pageInfo.size);
        QueryWrapper<ProductEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().isNull(ProductEntity::getDeletedAt); // 未删除
        if (options != null) {
            buildFilter(queryWrapper, options.getFilter());
            buildSortOrder(queryWrapper, options.getSort());
        }
        IPage<ProductEntity> productPage =
                this.productEntityMapper.selectPage(page, queryWrapper);

        ProductList productList = new ProductList();
        productList.setTotalItems((int) productPage.getTotal()); // 设置满足条件总记录数

        if (CollectionUtils.isEmpty(productPage.getRecords()))
            return productList; // 返回空

        // 将持久化实体类型转换成GraphQL传输类型
        productPage.getRecords().forEach(productEntity -> {
            Product product = BeanMapper.map(productEntity, Product.class);
            productList.getItems().add(product);
        });

        return productList;
    }

    private void buildSortOrder(QueryWrapper queryWrapper, ProductSortParameter sortParameter) {
        if (sortParameter == null) return ;
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getId(), "id");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getName(), "name");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getSlug(), "slug");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getDescription(), "description");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getUpdatedAt(), "updated_at");
    }

    private void buildFilter(QueryWrapper queryWrapper, ProductFilterParameter filterParameter) {
        if (filterParameter == null) return;
        QueryHelper.buildOneBooleanOperatorFilter(queryWrapper, filterParameter.getEnabled(), "enabled");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getName(), "name");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getSlug(), "slug");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getDescription(), "description");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getUpdatedAt(), "updated_at");
    }


    public ProductEntity findOne(Long productId) {
        QueryWrapper<ProductEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductEntity::getId, productId).isNull(ProductEntity::getDeletedAt);
        return this.productEntityMapper.selectOne(queryWrapper);
    }

    public List<ProductEntity> findByIds(List<Long> productIds) {
        return this.productEntityMapper.selectBatchIds(productIds);
    }

    public List<FacetValueEntity> getFacetValuesForProduct(Long productId) {
        QueryWrapper<ProductFacetValueJoinEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductFacetValueJoinEntity::getProductId, productId);
        List<ProductFacetValueJoinEntity> joinEntities =
                this.productFacetValueJoinEntityMapper.selectList(queryWrapper);
        List<Long> facetValueIds = joinEntities.stream().map(ProductFacetValueJoinEntity::getFacetValueId)
                .collect(Collectors.toList());

        QueryWrapper<FacetValueEntity> facetValueEntityQueryWrapper = new QueryWrapper<>();
        facetValueEntityQueryWrapper.lambda().in(FacetValueEntity::getId, facetValueIds);
        return this.facetValueEntityMapper.selectList(facetValueEntityQueryWrapper);
    }

    public ProductEntity findOneBySlug(String slug) {
        QueryWrapper<ProductEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductEntity::getSlug, slug);
        return this.productEntityMapper.selectOne(queryWrapper);
    }

    public ProductEntity create(RequestContext ctx, CreateProductInput input) {
        if (input.getSlug() != null) {
            String validatedSlug = this.validateSlug(input.getSlug(), null);
            input.setSlug(validatedSlug);
        }

        if (input.getFeaturedAssetId() != null) {
            // 校验assetId存在
            ServiceHelper.getEntityOrThrow(assetEntityMapper, AssetEntity.class, input.getFeaturedAssetId());
        }

        ProductEntity productEntity = BeanMapper.map(input, ProductEntity.class);
        this.productEntityMapper.insert(productEntity);

        if (!CollectionUtils.isEmpty(input.getFacetValueIds())) {
            this.joinProductWithFacetValue(input.getFacetValueIds(), productEntity.getId());
        }

        if (!CollectionUtils.isEmpty(input.getAssetIds())) {
            this.joinProductWithAsset(input.getAssetIds(), productEntity.getId());
        }

        ProductEvent event = new ProductEvent(ctx, productEntity, "created");
        this.eventBus.post(event);

        return productEntity;
    }

    public ProductEntity update(RequestContext ctx, UpdateProductInput input) {
        if (input.getSlug() != null) {
            String validatedSlug = this.validateSlug(input.getSlug(), input.getId());
            input.setSlug(validatedSlug);
        }
        ProductEntity existingProductEntity =
                ServiceHelper.getEntityOrThrow(this.productEntityMapper, ProductEntity.class, input.getId());

        if (input.getFeaturedAssetId() != null) {
            // 校验assetId存在
            ServiceHelper.getEntityOrThrow(assetEntityMapper, AssetEntity.class, input.getFeaturedAssetId());
        }

        BeanMapper.patch(input, existingProductEntity);
        this.productEntityMapper.updateById(existingProductEntity);

        if (input.getFacetValueIds() != null) {
            // 先清除现有关联关系
            QueryWrapper<ProductFacetValueJoinEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(ProductFacetValueJoinEntity::getProductId, existingProductEntity.getId());
            this.productFacetValueJoinEntityMapper.delete(queryWrapper);
            // 再添加
            if (input.getFacetValueIds().size() > 0) {
                this.joinProductWithFacetValue(input.getFacetValueIds(), existingProductEntity.getId());
            }
        }

        if (input.getAssetIds() != null) {
            // 先清除现有关联关系
            QueryWrapper<ProductAssetJoinEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(ProductAssetJoinEntity::getProductId, existingProductEntity.getId());
            this.productAssetJoinEntityMapper.delete(queryWrapper);
            // 再添加
            if (input.getAssetIds().size() > 0) {
                this.joinProductWithAsset(input.getAssetIds(), existingProductEntity.getId());
            }
        }

        ProductEvent event = new ProductEvent(ctx, existingProductEntity, "updated");
        this.eventBus.post(event);

        return existingProductEntity;
    }

    public DeletionResponse softDelete(RequestContext ctx, Long productId) {
        ProductEntity productEntity =
                ServiceHelper.getEntityOrThrow(this.productEntityMapper, ProductEntity.class, productId);
        productEntity.setDeletedAt(new Date());
        this.productEntityMapper.updateById(productEntity);

        ProductEvent event = new ProductEvent(ctx, productEntity, "deleted");
        this.eventBus.post(event);

        DeletionResponse deletionResponse = new DeletionResponse();
        deletionResponse.setResult(DeletionResult.DELETED);
        return deletionResponse;
    }

    public ProductEntity addOptionGroupToProduct(Long productId, Long optionGroupId) {
        // 确保productId存在
        ProductEntity productEntity =
                ServiceHelper.getEntityOrThrow(this.productEntityMapper, ProductEntity.class, productId);
        // 确保optionGroupId存在
        ServiceHelper.getEntityOrThrow(
                this.productOptionGroupEntityMapper, ProductOptionGroupEntity.class, optionGroupId);

        QueryWrapper<ProductOptionGroupJoinEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductOptionGroupJoinEntity::getProductId, productId)
                .eq(ProductOptionGroupJoinEntity::getOptionGroupId, optionGroupId);
        if (this.productOptionGroupJoinEntityMapper.selectCount(queryWrapper) == 0) { // 确保关联不存在
            ProductOptionGroupJoinEntity joinEntity = new ProductOptionGroupJoinEntity();
            joinEntity.setProductId(productId);
            joinEntity.setOptionGroupId(optionGroupId);
            this.productOptionGroupJoinEntityMapper.insert(joinEntity);
        }

        return productEntity;
    }

    public ProductEntity removeOptionGroupFromProduct(Long productId, Long optionGroupId) {
        // 确保productId存在
        ProductEntity productEntity =
                ServiceHelper.getEntityOrThrow(this.productEntityMapper, ProductEntity.class, productId);
        // 确保optionGroupId存在
        ProductOptionGroupEntity productOptionGroupEntity = ServiceHelper.getEntityOrThrow(
                this.productOptionGroupEntityMapper, ProductOptionGroupEntity.class, optionGroupId);

        QueryWrapper<ProductVariantEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductVariantEntity::getProductId, productId)
                .isNull(ProductVariantEntity::getDeletedAt);
        int variantCount = this.productVariantEntityMapper.selectCount(queryWrapper);
        if (variantCount > 0) {
            String errorMessage = "Cannot remove ProductOptionGroup \"{ " + productOptionGroupEntity.getCode() + " }\""
                    + " as it is used by { " + variantCount + " } ProductVariant(s)";
            throw new UserInputException(errorMessage);
        }

        QueryWrapper<ProductOptionGroupJoinEntity> productOptionGroupJoinEntityQueryWrapper = new QueryWrapper<>();
        productOptionGroupJoinEntityQueryWrapper.lambda().eq(ProductOptionGroupJoinEntity::getProductId, productId)
                .eq(ProductOptionGroupJoinEntity::getOptionGroupId, optionGroupId);
        this.productOptionGroupJoinEntityMapper.delete(productOptionGroupJoinEntityQueryWrapper);

        return productEntity;
    }

    private void joinProductWithFacetValue(List<Long> facetValueIds, Long productId) {
        QueryWrapper<FacetValueEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(FacetValueEntity::getId, facetValueIds)
                .select(FacetValueEntity::getId);
        List<Long> validFacetValueIds = this.facetValueEntityMapper.selectList(queryWrapper)
                .stream().map(FacetValueEntity::getId).collect(Collectors.toList());
        for(Long facetValueId : validFacetValueIds) {
            ProductFacetValueJoinEntity joinEntity = new ProductFacetValueJoinEntity();
            joinEntity.setProductId(productId);
            joinEntity.setFacetValueId(facetValueId);
            this.productFacetValueJoinEntityMapper.insert(joinEntity);
        }
    }

    private void joinProductWithAsset(List<Long> assetIds, Long productId) {
        QueryWrapper<AssetEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(AssetEntity::getId, assetIds)
                .select(AssetEntity::getId);
        List<Long> validAssetIds = this.assetEntityMapper.selectList(queryWrapper)
                .stream().map(AssetEntity::getId).collect(Collectors.toList());
        int pos = 0;
        for(Long assetId : validAssetIds) {
            ProductAssetJoinEntity joinEntity = new ProductAssetJoinEntity();
            joinEntity.setProductId(productId);
            joinEntity.setAssetId(assetId);
            joinEntity.setPosition(pos);
            this.productAssetJoinEntityMapper.insert(joinEntity);
            pos++;
        }
    }

    // 参考
    // https://stackoverflow.com/questions/1360113/is-java-regex-thread-safe
    private static Pattern alreadySuffixed = Pattern.compile("-\\d+$");

    /**
     * Normalizes the slug to be URL-safe, and ensure it is unique for the product.
     * Mutates the input.
     */
    private String validateSlug(String inputSlug, Long selfProductId) {
        String slug = NormalizeUtil.normalizeString(inputSlug, "-");
        int suffix = 1;
        boolean match = false;
        do {
            QueryWrapper<ProductEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(ProductEntity::getSlug, slug);
            if (selfProductId != null) {
                queryWrapper.lambda().ne(ProductEntity::getId, selfProductId);
            }
            match = this.productEntityMapper.selectCount(queryWrapper) > 0;
            if (match) {
                suffix++;
                if (alreadySuffixed.matcher(slug).find()) {
                    slug = slug.replaceFirst("-\\d+$", "-" + suffix);
                } else {
                    slug = slug + "-" + suffix;
                }
            }
        } while (match);
        return slug;
    }

}


