/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.common.utils.SamplesEach;
import io.geekstore.entity.*;
import io.geekstore.eventbus.events.ProductVariantEvent;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class ProductVariantService {
    private final ProductVariantEntityMapper productVariantEntityMapper;
    private final ProductVariantCollectionJoinEntityMapper productVariantCollectionJoinEntityMapper;
    private final ProductEntityMapper productEntityMapper;
    private final ProductVariantProductOptionJoinEntityMapper productVariantProductOptionJoinEntityMapper;
    private final ProductOptionEntityMapper productOptionEntityMapper;
    private final ProductVariantFacetValueJoinEntityMapper productVariantFacetValueJoinEntityMapper;
    private final FacetValueEntityMapper facetValueEntityMapper;
    private final ProductOptionGroupEntityMapper productOptionGroupEntityMapper;
    private final AssetEntityMapper assetEntityMapper;
    private final ProductVariantAssetJoinEntityMapper productVariantAssetJoinEntityMapper;
    private final ProductOptionGroupJoinEntityMapper productOptionGroupJoinEntityMapper;

    private final GlobalSettingsService globalSettingsService;
    private final StockMovementService stockMovementService;

    private final EventBus eventBus;

    public ProductVariantEntity findOne(Long productVariantId) {
        return productVariantEntityMapper.selectById(productVariantId);
    }

    public List<ProductVariantEntity> findByIds(List<Long> ids) {
        return productVariantEntityMapper.selectBatchIds(ids);
    }

    public List<ProductVariantEntity> getVariantsByProductId(Long productId) {
        QueryWrapper<ProductVariantEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductVariantEntity::getProductId, productId)
                .isNull(ProductVariantEntity::getDeletedAt).orderByAsc(ProductVariantEntity::getId);
        return productVariantEntityMapper.selectList(queryWrapper);
    }

    public ProductVariantList getVariantsByCollectionId(Long collectionId, ProductVariantListOptions options) {
        ProductVariantList variantList = new ProductVariantList();

        List<Long> variantIds = getAvailableProductVariantIds(collectionId, options);
        if (CollectionUtils.isEmpty(variantIds)) {
            variantList.setTotalItems(0);
            return variantList;
        }

        PageInfo pageInfo = ServiceHelper.getListOptions(options);
        IPage<ProductVariantEntity> page = new Page<>(pageInfo.current, pageInfo.size);
        QueryWrapper<ProductVariantEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().isNull(ProductVariantEntity::getDeletedAt); // 未删除
        queryWrapper.lambda().in(ProductVariantEntity::getId, variantIds);
        if (options != null) {
            buildFilter(queryWrapper, options.getFilter());
            buildSortOrder(queryWrapper, options.getSort());
        }
        IPage<ProductVariantEntity> variantPage =
                this.productVariantEntityMapper.selectPage(page, queryWrapper);

        variantList.setTotalItems((int) variantPage.getTotal()); // 设置满足条件总记录数

        if (CollectionUtils.isEmpty(variantPage.getRecords()))
            return variantList; // 返回空

        // 将持久化实体类型转换成GraphQL传输类型
        variantPage.getRecords().forEach(variantEntity -> {
            ProductVariant productVariant = BeanMapper.map(variantEntity, ProductVariant.class);
            variantList.getItems().add(productVariant);
        });

        return variantList;
    }

    private void buildSortOrder(QueryWrapper queryWrapper, ProductVariantSortParameter sortParameter) {
        if (sortParameter == null) return ;
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getId(), "id");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getStockOnHand(), "stock_on_hand");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getProductId(), "product_id");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getSku(), "sku");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getName(), "name");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getPrice(), "price");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getUpdatedAt(), "updated_at");
    }

    private void buildFilter(QueryWrapper queryWrapper, ProductVariantFilterParameter filterParameter) {
        if (filterParameter == null) return;
        QueryHelper.buildOneBooleanOperatorFilter(queryWrapper, filterParameter.getEnabled(), "enabled");
        QueryHelper.buildOneNumberOperatorFilter(queryWrapper, filterParameter.getStockOnHand(), "stock_on_hand");
        QueryHelper.buildOneBooleanOperatorFilter(queryWrapper, filterParameter.getTrackInventory(), "track_inventory");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getSku(), "sku");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getName(), "name");
        QueryHelper.buildOneNumberOperatorFilter(queryWrapper, filterParameter.getPrice(), "price");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getUpdatedAt(), "updated_at");
    }

    private List<Long> getAvailableProductVariantIds(Long collectionId, ProductVariantListOptions options) {
        QueryWrapper<ProductVariantCollectionJoinEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductVariantCollectionJoinEntity::getCollectionId, collectionId);
        List<ProductVariantCollectionJoinEntity> joinEntities =
                this.productVariantCollectionJoinEntityMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(joinEntities)) {
            return new ArrayList<>();
        }

        List<Long> variantIds = joinEntities.stream()
                .map(ProductVariantCollectionJoinEntity::getProductVariantId).distinct().collect(Collectors.toList());

        QueryWrapper<ProductVariantEntity> productVariantQueryWrapper = new QueryWrapper<>();
        productVariantQueryWrapper.lambda()
                .in(ProductVariantEntity::getId, variantIds)
                .select(ProductVariantEntity::getId, ProductVariantEntity::getProductId);
        List<ProductVariantEntity> variants =
                this.productVariantEntityMapper.selectList(productVariantQueryWrapper);

        List<Long> productIds = variants.stream().map(ProductVariantEntity::getProductId)
                .collect(Collectors.toList());
        QueryWrapper<ProductEntity> productEntityQueryWrapper = new QueryWrapper<>();
        // 查找已经删除(或不enabled)的Product
        productEntityQueryWrapper.lambda().in(ProductEntity::getId, productIds)
                .isNotNull(ProductEntity::getDeletedAt).select(ProductEntity::getId);
        if (options != null && options.getFilter() != null && options.getFilter().getEnabled() != null &&
            options.getFilter().getEnabled().getEq()) {
            productEntityQueryWrapper.lambda().or().eq(ProductEntity::isEnabled, false);
        }
        List<ProductEntity> notAvailableProducts = this.productEntityMapper.selectList(productEntityQueryWrapper);
        List<Long> availableVariantIds = null;
        if (!CollectionUtils.isEmpty(notAvailableProducts)) {
            Set<Long> notAvailableProductIds =
                    notAvailableProducts.stream().map(ProductEntity::getId).collect(Collectors.toSet());
            availableVariantIds = variants.stream()
                    .filter(variant -> !notAvailableProductIds.contains(variant.getProductId()))
                    .map(ProductVariantEntity::getId).collect(Collectors.toList());
        } else {
            availableVariantIds = variants.stream()
                    .map(ProductVariantEntity::getId).collect(Collectors.toList());
        }

        return availableVariantIds;
    }

    public List<ProductOptionEntity> getOptionsForVariant(Long variantId) {
        QueryWrapper<ProductVariantProductOptionJoinEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductVariantProductOptionJoinEntity::getProductVariantId, variantId);
        List<ProductVariantProductOptionJoinEntity> joinEntities =
                this.productVariantProductOptionJoinEntityMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(joinEntities)) {
            return new ArrayList<>();
        }

        List<Long> optionIds = joinEntities.stream()
                .map(ProductVariantProductOptionJoinEntity::getProductOptionId).collect(Collectors.toList());
        QueryWrapper<ProductOptionEntity> productOptionEntityQueryWrapper = new QueryWrapper<>();
        productOptionEntityQueryWrapper.lambda().in(ProductOptionEntity::getId, optionIds);
        List<ProductOptionEntity> optionEntities =
                this.productOptionEntityMapper.selectList(productOptionEntityQueryWrapper);
        return optionEntities;
    }

    public List<FacetValueEntity> getFacetValuesForVariant(Long variantId) {
        QueryWrapper<ProductVariantFacetValueJoinEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductVariantFacetValueJoinEntity::getProductVariantId, variantId);
        List<ProductVariantFacetValueJoinEntity> joinEntities =
                this.productVariantFacetValueJoinEntityMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(joinEntities)) {
            return new ArrayList<>();
        }

        List<Long> facetValueIds = joinEntities.stream()
                .map(ProductVariantFacetValueJoinEntity::getFacetValueId).collect(Collectors.toList());
        QueryWrapper<FacetValueEntity> facetValueEntityQueryWrapper = new QueryWrapper<>();
        facetValueEntityQueryWrapper.lambda().in(FacetValueEntity::getId, facetValueIds);
        List<FacetValueEntity> facetValueEntities =
                this.facetValueEntityMapper.selectList(facetValueEntityQueryWrapper);
        return facetValueEntities;
    }

    public ProductEntity getProductForVariant(ProductVariantEntity productVariantEntity) {
        ProductEntity productEntity = ServiceHelper.getEntityOrThrow(
                this.productEntityMapper, ProductEntity.class, productVariantEntity.getProductId());
        return productEntity;
    }

    public List<ProductVariantEntity> create(RequestContext ctx, List<CreateProductVariantInput> inputs) {
        List<ProductVariantEntity> createdVariants = new ArrayList<>();
        for(CreateProductVariantInput input : inputs) {
            ProductVariantEntity createdVariant = this.createSingle(input);
            createdVariants.add(createdVariant);
        }
        ProductVariantEvent event = new ProductVariantEvent(ctx, createdVariants, "created");
        this.eventBus.post(event);
        return createdVariants;
    }

    public List<ProductVariantEntity> update(RequestContext ctx, List<UpdateProductVariantInput> inputs) {
        List<ProductVariantEntity> updatedVariants = new ArrayList<>();
        for(UpdateProductVariantInput input : inputs) {
            ProductVariantEntity updatedVariant = this.updateSingle(input);
            updatedVariants.add(updatedVariant);
        }
        ProductVariantEvent event = new ProductVariantEvent(ctx, updatedVariants, "updated");
        this.eventBus.post(event);
        return updatedVariants;
    }

    public DeletionResponse softDelete(RequestContext ctx, Long id) {
        ProductVariantEntity variant =
                ServiceHelper.getEntityOrThrow(this.productVariantEntityMapper, ProductVariantEntity.class, id);
        variant.setDeletedAt(new Date());
        this.productVariantEntityMapper.updateById(variant);
        ProductVariantEvent event = new ProductVariantEvent(ctx, Arrays.asList(variant), "deleted");
        this.eventBus.post(event);
        DeletionResponse deletionResponse = new DeletionResponse();
        deletionResponse.setResult(DeletionResult.DELETED);
        return deletionResponse;
    }

    public ProductVariantEntity updateSingle(UpdateProductVariantInput input) {
        ProductVariantEntity existingVariant = ServiceHelper.getEntityOrThrow(
                this.productVariantEntityMapper, ProductVariantEntity.class, input.getId());
        Integer oldStockLevel = existingVariant.getStockOnHand();
        if (input.getStockOnHand() != null && input.getStockOnHand() < 0) {
            throw new UserInputException("stockOnHand cannot be a negative value");
        }
        if (input.getFeaturedAssetId() != null) {
            // 校验assetId存在
            ServiceHelper.getEntityOrThrow(this.assetEntityMapper, AssetEntity.class, input.getFeaturedAssetId());
        }
        BeanMapper.patch(input, existingVariant);
        this.productVariantEntityMapper.updateById(existingVariant);

        if (!CollectionUtils.isEmpty(input.getFacetValueIds())) {
            // 删除现有关联关系
            QueryWrapper<ProductVariantFacetValueJoinEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(ProductVariantFacetValueJoinEntity::getProductVariantId, existingVariant.getId());
            this.productVariantFacetValueJoinEntityMapper.delete(queryWrapper);
            // 添加更新的关联关系
            this.joinVariantWithFacetValue(input.getFacetValueIds(), existingVariant.getId());
        }

        if (!CollectionUtils.isEmpty(input.getAssetIds())) {
            // 删除现有关联关系
            QueryWrapper<ProductVariantAssetJoinEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(ProductVariantAssetJoinEntity::getProductVariantId, existingVariant.getId());
            this.productVariantAssetJoinEntityMapper.delete(queryWrapper);
            // 添加更新的关联关系
            this.joinVariantWithAsset(input.getAssetIds(), existingVariant.getId());
        }

        if (input.getStockOnHand() != null) {
            this.stockMovementService.adjustProductVariantStock(
                    existingVariant.getId(),
                    oldStockLevel,
                    input.getStockOnHand()
            );
        }
        return existingVariant;
    }

    @Transactional
    ProductVariantEntity createSingle(CreateProductVariantInput input) {
        this.validateVariantOptionIds(input);
        if (input.getFeaturedAssetId() != null) {
            // 校验assetId存在
            ServiceHelper.getEntityOrThrow(this.assetEntityMapper, AssetEntity.class, input.getFeaturedAssetId());
        }
        if (input.getPrice() == null) {
            input.setPrice(0);
        }
        if (input.getTrackInventory() == null) {
            boolean trackInventory = this.globalSettingsService.getSettings().isTrackInventory();
            input.setTrackInventory(trackInventory);
        }

        ProductVariantEntity createdVariant = BeanMapper.patch(input, ProductVariantEntity.class);
        this.productVariantEntityMapper.insert(createdVariant);

        if (!CollectionUtils.isEmpty(input.getOptionIds())) {
            QueryWrapper<ProductOptionEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(ProductOptionEntity::getId, input.getOptionIds())
                    .select(ProductOptionEntity::getId);
            List<Long> validOptionIds = this.productOptionEntityMapper.selectList(queryWrapper)
                    .stream().map(ProductOptionEntity::getId).collect(Collectors.toList());
            for(Long optionId : validOptionIds) {
                ProductVariantProductOptionJoinEntity joinEntity = new ProductVariantProductOptionJoinEntity();
                joinEntity.setProductVariantId(createdVariant.getId());
                joinEntity.setProductOptionId(optionId);
                this.productVariantProductOptionJoinEntityMapper.insert(joinEntity);
            }
        }

        if (!CollectionUtils.isEmpty(input.getFacetValueIds())) {
            this.joinVariantWithFacetValue(input.getFacetValueIds(), createdVariant.getId());
        }

        if (!CollectionUtils.isEmpty(input.getAssetIds())) {
            this.joinVariantWithAsset(input.getAssetIds(), createdVariant.getId());
        }

        if (input.getStockOnHand() != null && input.getStockOnHand() != 0) {
            this.stockMovementService.adjustProductVariantStock(
                    createdVariant.getId(),
                    0,
                    input.getStockOnHand()
            );
        }

        return createdVariant;
    }

    private void joinVariantWithFacetValue(List<Long> facetValueIds, Long variantId) {
        QueryWrapper<FacetValueEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(FacetValueEntity::getId, facetValueIds)
                .select(FacetValueEntity::getId);
        List<Long> validFacetValueIds = this.facetValueEntityMapper.selectList(queryWrapper)
                .stream().map(FacetValueEntity::getId).collect(Collectors.toList());
        for(Long facetValueId : validFacetValueIds) {
            ProductVariantFacetValueJoinEntity joinEntity = new ProductVariantFacetValueJoinEntity();
            joinEntity.setProductVariantId(variantId);
            joinEntity.setFacetValueId(facetValueId);
            this.productVariantFacetValueJoinEntityMapper.insert(joinEntity);
        }
    }

    private void joinVariantWithAsset(List<Long> assetIds, Long variantId) {
        QueryWrapper<AssetEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(AssetEntity::getId, assetIds)
                .select(AssetEntity::getId);
        List<Long> validAssetIds = this.assetEntityMapper.selectList(queryWrapper)
                .stream().map(AssetEntity::getId).collect(Collectors.toList());
        int pos = 0;
        for(Long assetId : validAssetIds) {
            ProductVariantAssetJoinEntity joinEntity = new ProductVariantAssetJoinEntity();
            joinEntity.setProductVariantId(variantId);
            joinEntity.setAssetId(assetId);
            joinEntity.setPosition(pos);
            this.productVariantAssetJoinEntityMapper.insert(joinEntity);
            pos++;
        }
    }

    private void validateVariantOptionIds(CreateProductVariantInput input) {
        QueryWrapper<ProductOptionGroupJoinEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductOptionGroupJoinEntity::getProductId, input.getProductId());
        List<ProductOptionGroupJoinEntity> productOptionGroupJoinEntities =
                this.productOptionGroupJoinEntityMapper.selectList(queryWrapper);
        List<Long> productGroupIds = productOptionGroupJoinEntities.stream()
                .map(ProductOptionGroupJoinEntity::getOptionGroupId).distinct().sorted().collect(Collectors.toList());
        List<ProductOptionGroupEntity> optionGroupEntities =
                this.productOptionGroupEntityMapper.selectBatchIds(productGroupIds);

        if (input.getOptionIds().size() != optionGroupEntities.size()) {
            this.throwIncompatibleOptionsError(optionGroupEntities);
        }

        if (CollectionUtils.isEmpty(input.getOptionIds())) return;

        List<List<Long>> groupOptionsMapping = this.getGroups(optionGroupEntities);
        if (!SamplesEach.samplesEach(input.getOptionIds(), groupOptionsMapping)) {
            this.throwIncompatibleOptionsError(optionGroupEntities);
        }

        String inputOptionIds = input.getOptionIds()
                .stream().map(id -> id.toString()).sorted().collect(Collectors.joining(","));

        Map<Long, List<ProductOptionEntity>> variantOptionsMap =
                this.getProductVariantOptionsByProductId(input.getProductId());
        variantOptionsMap.forEach((variantId, options) -> {
            String variantOptionIds = options.stream().map(option -> option.getId().toString())
                    .sorted().collect(Collectors.joining(","));
            if (variantOptionIds.equals(inputOptionIds)) {
                String optionNames = options.stream().map(ProductOptionEntity::getCode).sorted()
                        .collect(Collectors.joining(", "));
                String errorMessage = "A ProductVariant already exists with the options: " +
                        "{ " + optionNames + " }";
                throw new UserInputException(errorMessage);
            }
        });
    }

    private Map<Long, List<ProductOptionEntity>> getProductVariantOptionsByProductId(Long productId) {
        QueryWrapper<ProductVariantEntity> productVariantEntityQueryWrapper = new QueryWrapper<>();
        productVariantEntityQueryWrapper.lambda().eq(ProductVariantEntity::getProductId, productId)
                .isNull(ProductVariantEntity::getDeletedAt).select(ProductVariantEntity::getId);
        List<ProductVariantEntity> variantEntities =
                this.productVariantEntityMapper.selectList(productVariantEntityQueryWrapper);
        List<Long> productVariantIds = variantEntities.stream().map(ProductVariantEntity::getId).distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productVariantIds)) return new HashMap<>();

        QueryWrapper<ProductVariantProductOptionJoinEntity> productVariantProductOptionJoinEntityQueryWrapper =
                new QueryWrapper<>();
        productVariantProductOptionJoinEntityQueryWrapper.
                lambda().in(ProductVariantProductOptionJoinEntity::getProductVariantId, productVariantIds);
        List<ProductVariantProductOptionJoinEntity> productVariantProductOptionJoinEntities =
                this.productVariantProductOptionJoinEntityMapper
                        .selectList(productVariantProductOptionJoinEntityQueryWrapper);
        if (CollectionUtils.isEmpty(productVariantProductOptionJoinEntities)) return new HashMap<>();


        List<Long> productOptionIds = productVariantProductOptionJoinEntities.stream()
                .map(ProductVariantProductOptionJoinEntity::getProductOptionId).collect(Collectors.toList());
        QueryWrapper<ProductOptionEntity> productOptionEntityQueryWrapper = new QueryWrapper<>();
        productOptionEntityQueryWrapper.lambda().in(ProductOptionEntity::getId, productOptionIds);
        List<ProductOptionEntity> productOptionEntities =
                this.productOptionEntityMapper.selectList(productOptionEntityQueryWrapper);
        Map<Long, ProductOptionEntity> productOptionEntityMap = productOptionEntities.stream()
                .collect(toMap(ProductOptionEntity::getId, productOptionEntity -> productOptionEntity));

        Map<Long, List<ProductOptionEntity>> resultMap = new HashMap<>();
        for(ProductVariantProductOptionJoinEntity joinEntity : productVariantProductOptionJoinEntities) {
            Long variantId = joinEntity.getProductVariantId();
            Long optionId = joinEntity.getProductOptionId();
            ProductOptionEntity optionEntity = productOptionEntityMap.get(optionId);
            resultMap.computeIfAbsent(variantId, k -> new ArrayList<>()).add(optionEntity);
        }

        return resultMap;
    }

    private List<List<Long>> getGroups(List<ProductOptionGroupEntity> optionGroupEntities) {
        List<Long> optionGroupIds = optionGroupEntities.stream().map(ProductOptionGroupEntity::getId)
                .collect(Collectors.toList());
        QueryWrapper<ProductOptionEntity> productOptionEntityQueryWrapper = new QueryWrapper<>();
        productOptionEntityQueryWrapper.lambda().in(ProductOptionEntity::getGroupId, optionGroupIds);
        List<ProductOptionEntity> productOptionEntities =
                this.productOptionEntityMapper.selectList(productOptionEntityQueryWrapper);
        Map<Long, List<Long>> groupByGroupId =
                productOptionEntities.stream().collect(Collectors.groupingBy(ProductOptionEntity::getGroupId,
                Collectors.mapping(ProductOptionEntity::getId, Collectors.toList())));
        List<List<Long>> groups = new ArrayList<>(groupByGroupId.values());
        return groups;
    }

    private void throwIncompatibleOptionsError(List<ProductOptionGroupEntity> optionGroupEntities) {
        String groupNames = optionGroupEntities.stream().map(ProductOptionGroupEntity::getCode).sorted()
                .collect(Collectors.joining(", "));
        String errorMessage = "ProductVariant optionIds must include one optionId from each of the groups: " +
                "{ " + groupNames + " }";
        throw new UserInputException(errorMessage);
    }
}
