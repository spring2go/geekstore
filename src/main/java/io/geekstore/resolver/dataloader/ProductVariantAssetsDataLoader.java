/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.AssetEntity;
import io.geekstore.entity.ProductVariantAssetJoinEntity;
import io.geekstore.mapper.AssetEntityMapper;
import io.geekstore.mapper.ProductVariantAssetJoinEntityMapper;
import io.geekstore.types.asset.Asset;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dataloader.MappedBatchLoader;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
@SuppressWarnings("Duplicates")
@RequiredArgsConstructor
public class ProductVariantAssetsDataLoader implements MappedBatchLoader<Long, List<Asset>> {
    private final ProductVariantAssetJoinEntityMapper productVariantAssetJoinEntityMapper;
    private final AssetEntityMapper assetEntityMapper;


    @Override
    public CompletionStage<Map<Long, List<Asset>>> load(Set<Long> productVariantIds) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Long, List<Asset>> variantAssetsMap = new HashMap<>();
            productVariantIds.forEach(id -> variantAssetsMap.put(id, new ArrayList<>()));

            QueryWrapper<ProductVariantAssetJoinEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(ProductVariantAssetJoinEntity::getProductVariantId, productVariantIds);
            List<ProductVariantAssetJoinEntity> joinEntities =
                    productVariantAssetJoinEntityMapper.selectList(queryWrapper);
            if (CollectionUtils.isEmpty(joinEntities)) return variantAssetsMap;

            Set<Long> assetIds = joinEntities.stream()
                    .map(ProductVariantAssetJoinEntity::getAssetId).collect(Collectors.toSet());
            List<AssetEntity> assetEntityList = assetEntityMapper.selectBatchIds(assetIds);
            if (CollectionUtils.isEmpty(assetEntityList)) return variantAssetsMap;

            Map<Long, AssetEntity> assetEntityMap = assetEntityList.stream()
                    .collect(Collectors.toMap(AssetEntity::getId, assetEntity -> assetEntity));

            joinEntities.forEach(joinEntity -> {
                Long productVariantId = joinEntity.getProductVariantId();
                Long assetId = joinEntity.getAssetId();
                List<Asset> assetList = variantAssetsMap.get(productVariantId);
                AssetEntity assetEntity = assetEntityMap.get(assetId);
                Asset asset = BeanMapper.patch(assetEntity, Asset.class);
                assetList.add(asset);
            });

            return variantAssetsMap;
        });
    }
}
