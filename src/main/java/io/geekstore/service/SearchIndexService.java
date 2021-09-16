/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.entity.*;
import io.geekstore.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This service is responsible for search index updates.
 *
 * Created on Jan, 2021 by @author bobo
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class SearchIndexService {
    private final SearchIndexItemEntityMapper searchIndexItemEntityMapper;
    private final ProductVariantFacetValueJoinEntityMapper productVariantFacetValueJoinEntityMapper;
    private final ProductFacetValueJoinEntityMapper productFacetValueJoinEntityMapper;
    private final FacetValueEntityMapper facetValueEntityMapper;
    private final ProductEntityMapper productEntityMapper;
    private final AssetEntityMapper assetEntityMapper;
    private final ProductVariantCollectionJoinEntityMapper productVariantCollectionJoinEntityMapper;
    private final CollectionEntityMapper collectionEntityMapper;
    private final ProductVariantEntityMapper productVariantEntityMapper;

    public boolean reindex() {
        List<ProductVariantEntity> productVariants = getAllValidProductVariants();
        log.info("Reindexing " + productVariants.size() + " variants");

        this.searchIndexItemEntityMapper.delete(new QueryWrapper<>());
        log.info("Deleted existing index items");

        this.saveVariants(productVariants);
        log.info("Completed reindexing");

        return true;
    }

    private List<ProductVariantEntity> getAllValidProductVariants() {
        QueryWrapper<ProductVariantEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().isNull(ProductVariantEntity::getDeletedAt); // 未删除
        List<ProductVariantEntity> productVariants = this.productVariantEntityMapper.selectList(queryWrapper);

        if (CollectionUtils.isEmpty(productVariants)) return new ArrayList<>();

        List<Long> productIds = productVariants.stream()
                .map(ProductVariantEntity::getProductId).collect(Collectors.toList());

        QueryWrapper<ProductEntity> productEntityQueryWrapper = new QueryWrapper<>();
        productEntityQueryWrapper.lambda().in(ProductEntity::getId, productIds)
                .isNotNull(ProductEntity::getDeletedAt) // 已删除
                .select(ProductEntity::getId);
        Set<Long> deletedProductIds = this.productEntityMapper.selectList(productEntityQueryWrapper)
                .stream().map(ProductEntity::getId).collect(Collectors.toSet());

        if (!CollectionUtils.isEmpty(deletedProductIds)) {
            // 确保对应的Product没有删除
            productVariants = productVariants.stream()
                    .filter(v -> !deletedProductIds.contains(v.getProductId())).collect(Collectors.toList());
        }
        return productVariants;
    }

    public boolean updateProduct(Long productId) {
        return _updateProduct(productId);
    }

    public boolean updateVariants(List<Long> variantIds) {
        return this._updateVariants(variantIds);
    }

    public boolean deleteProduct(Long productId) {
        return this._deleteProduct(productId);
    }

    public boolean deleteVariants(List<Long> variantIds) {
        this.removeSearchIndexItems(variantIds);
        return true;
    }

    public boolean deleteAsset(Long assetId) {
        QueryWrapper<SearchIndexItemEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SearchIndexItemEntity::getProductAssetId, assetId);
        List<SearchIndexItemEntity> indexItems = this.searchIndexItemEntityMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(indexItems)) {
            indexItems.forEach(item -> item.setProductPreviewFocalPoint(null));
            indexItems.forEach(item -> this.searchIndexItemEntityMapper.updateById(item));
        }

        queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SearchIndexItemEntity::getProductVariantAssetId, assetId);
        indexItems = this.searchIndexItemEntityMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(indexItems)) {
            indexItems.forEach(item -> item.setProductPreviewFocalPoint(null));
            indexItems.forEach(item -> this.searchIndexItemEntityMapper.updateById(item));
        }
        return true;
    }

    public boolean updateAsset(AssetEntity asset) {
        QueryWrapper<SearchIndexItemEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SearchIndexItemEntity::getProductAssetId, asset.getId());
        List<SearchIndexItemEntity> indexItems = this.searchIndexItemEntityMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(indexItems)) {
            indexItems.forEach(item -> item.setProductPreviewFocalPoint(asset.getFocalPoint()));
            indexItems.forEach(item -> this.searchIndexItemEntityMapper.updateById(item));
        }

        queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SearchIndexItemEntity::getProductVariantAssetId, asset.getId());
        indexItems = this.searchIndexItemEntityMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(indexItems)) {
            indexItems.forEach(item -> item.setProductPreviewFocalPoint(asset.getFocalPoint()));
            indexItems.forEach(item -> this.searchIndexItemEntityMapper.updateById(item));
        }
        return true;
    }

    private boolean _updateProduct(Long productId) {
        ProductEntity product = this.productEntityMapper.selectById(productId);
        if (product != null) {
            QueryWrapper<ProductVariantEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(ProductVariantEntity::getProductId, productId)
                    .isNull(ProductVariantEntity::getDeletedAt);
            List<ProductVariantEntity> variants = this.productVariantEntityMapper.selectList(queryWrapper);
            if (!product.isEnabled()) {
                variants.forEach(v -> v.setEnabled(false));
            }
            log.info("Updating " + variants.size() + " variants");
            if (variants.size() > 0) {
                this.saveVariants(variants);
            }
        }
        return true;
    }

    private boolean _updateVariants(List<Long> variantIds) {
        QueryWrapper<ProductVariantEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(ProductVariantEntity::getId, variantIds)
                .isNull(ProductVariantEntity::getDeletedAt);
        List<ProductVariantEntity> variants = this.productVariantEntityMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(variantIds)) {
            this.saveVariants(variants);
        }
        return true;
    }

    private boolean _deleteProduct(Long productId) {
        QueryWrapper<ProductVariantEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductVariantEntity::getProductId, productId)
                .select(ProductVariantEntity::getId);
        List<Long> productVariantIds = this.productVariantEntityMapper.selectList(queryWrapper)
                .stream().map(ProductVariantEntity::getId)
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(productVariantIds)) {
            this.removeSearchIndexItems(productVariantIds);
        }
        return true;
    }

    private void saveVariants(List<ProductVariantEntity> variants) {
        for(ProductVariantEntity variant : variants) {
            SearchIndexItemEntity item = new SearchIndexItemEntity();
            item.setProductVariantId(variant.getId());
            item.setSku(variant.getSku());

            ProductEntity product = productEntityMapper.selectById(variant.getProductId());
            item.setEnabled(product.isEnabled() ? variant.isEnabled(): false);
            item.setSlug(product.getSlug());
            item.setPrice(variant.getPrice());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setDescription(product.getDescription());
            item.setProductVariantName(variant.getName());

            if (product.getFeaturedAssetId() != null) {
                AssetEntity featuredAsset = assetEntityMapper.selectById(product.getFeaturedAssetId());
                item.setProductAssetId(featuredAsset.getId());
                item.setProductPreviewFocalPoint(featuredAsset.getFocalPoint());
                item.setProductPreview(featuredAsset.getPreview());
            }
            if (variant.getFeaturedAssetId() != null) {
                AssetEntity featuredAsset = assetEntityMapper.selectById(variant.getFeaturedAssetId());
                item.setProductVariantPreviewFocalPoint(featuredAsset.getFocalPoint());
                item.setProductVariantAssetId(featuredAsset.getId());
                item.setProductPreview(featuredAsset.getPreview());
            }
            item.setFacetIds(this.getFacetIds(variant));
            item.setFacetValueIds(this.getFacetValueIds(variant));

            List<CollectionEntity> collections = this.getCollections(variant.getId());
            item.setCollectionIds(collections.stream().map(CollectionEntity::getId).collect(Collectors.toList()));
            item.setCollectionSlugs(collections.stream().map(CollectionEntity::getSlug).collect(Collectors.toList()));

            if (checkExists(item.getProductVariantId())) {
                this.searchIndexItemEntityMapper.updateById(item);
            } else {
                this.searchIndexItemEntityMapper.insert(item);
            }
        }
    }

    private boolean checkExists(Long productVariantId) {
        QueryWrapper<SearchIndexItemEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SearchIndexItemEntity::getProductVariantId, productVariantId);
        return this.searchIndexItemEntityMapper.selectCount(queryWrapper) > 0;
    }

    private List<CollectionEntity> getCollections(Long productVariantId) {
        QueryWrapper<ProductVariantCollectionJoinEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductVariantCollectionJoinEntity::getProductVariantId, productVariantId);

        List<Long> collectionIds = this.productVariantCollectionJoinEntityMapper.selectList(queryWrapper)
                .stream().map(ProductVariantCollectionJoinEntity::getCollectionId)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(collectionIds)) return new ArrayList<>();

        QueryWrapper<CollectionEntity> collectionEntityQueryWrapper = new QueryWrapper<>();
        collectionEntityQueryWrapper.lambda().in(CollectionEntity::getId, collectionIds);
        collectionEntityQueryWrapper.lambda().select(CollectionEntity::getId, CollectionEntity::getSlug);
        return this.collectionEntityMapper.selectList(collectionEntityQueryWrapper);
    }

    private List<Long> getFacetIds(ProductVariantEntity productVariant) {
        List<Long> facetValueIds = this.getFacetValueIds(productVariant);
        if (CollectionUtils.isEmpty(facetValueIds)) return new ArrayList<>();

        QueryWrapper<FacetValueEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(FacetValueEntity::getId, facetValueIds).select(FacetValueEntity::getFacetId);
        List<Long> facetIds = this.facetValueEntityMapper.selectList(queryWrapper)
                .stream().map(FacetValueEntity::getFacetId)
                .collect(Collectors.toList());

        return facetIds;
    }

    private List<Long> getFacetValueIds(ProductVariantEntity productVariant) {
        QueryWrapper<ProductVariantFacetValueJoinEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductVariantFacetValueJoinEntity::getProductVariantId, productVariant.getId());
        List<Long> variantFacetValueIds = this.productVariantFacetValueJoinEntityMapper
                .selectList(queryWrapper).stream()
                .map(ProductVariantFacetValueJoinEntity::getFacetValueId)
                .collect(Collectors.toList());

        QueryWrapper<ProductFacetValueJoinEntity> productFacetValueJoinEntityQueryWrapper = new QueryWrapper<>();
        productFacetValueJoinEntityQueryWrapper.lambda()
                .eq(ProductFacetValueJoinEntity::getProductId, productVariant.getProductId());
        List<Long> productFacetValueIds = this.productFacetValueJoinEntityMapper
                .selectList(productFacetValueJoinEntityQueryWrapper).stream()
                .map(ProductFacetValueJoinEntity::getFacetValueId)
                .collect(Collectors.toList());

        Set<Long> set = new HashSet<>();
        set.addAll(variantFacetValueIds);
        set.addAll(productFacetValueIds);

        return new ArrayList<>(set);
    }

    private void removeSearchIndexItems(List<Long> variantIds) {
        this.searchIndexItemEntityMapper.deleteBatchIds(variantIds);
    }
}
