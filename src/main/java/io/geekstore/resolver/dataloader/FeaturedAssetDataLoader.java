/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.AssetEntity;
import io.geekstore.mapper.AssetEntityMapper;
import io.geekstore.types.asset.Asset;
import org.dataloader.MappedBatchLoader;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */

public class FeaturedAssetDataLoader implements MappedBatchLoader<Long, Asset> {
    private final AssetEntityMapper assetEntityMapper;

    public FeaturedAssetDataLoader(AssetEntityMapper assetEntityMapper) {
        this.assetEntityMapper = assetEntityMapper;
    }

    @Override
    public CompletionStage<Map<Long, Asset>> load(Set<Long> assetIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<AssetEntity> assetEntityList = this.assetEntityMapper.selectBatchIds(assetIds);
            List<Asset> assetList = assetEntityList.stream()
                    .map(assetEntity -> BeanMapper.map(assetEntity, Asset.class)).collect(Collectors.toList());
            Map<Long, Asset> featuredAssetMap = assetList.stream()
                    .collect(Collectors.toMap(Asset::getId, asset -> asset));
            return featuredAssetMap;
        });
    }
}
