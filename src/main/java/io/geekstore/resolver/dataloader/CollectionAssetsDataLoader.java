/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.AssetEntity;
import io.geekstore.entity.CollectionAssetJoinEntity;
import io.geekstore.mapper.AssetEntityMapper;
import io.geekstore.mapper.CollectionAssetJoinEntityMapper;
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
public class CollectionAssetsDataLoader implements MappedBatchLoader<Long, List<Asset>> {

    private final CollectionAssetJoinEntityMapper collectionAssetJoinEntityMapper;
    private final AssetEntityMapper assetEntityMapper;

    @Override
    public CompletionStage<Map<Long, List<Asset>>> load(Set<Long> collectionIds) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Long, List<Asset>> collectionAssetsMap = new HashMap<>();
            collectionIds.forEach(id -> collectionAssetsMap.put(id, new ArrayList<>()));

            QueryWrapper<CollectionAssetJoinEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(CollectionAssetJoinEntity::getCollectionId, collectionIds);
            List<CollectionAssetJoinEntity> collectionAssetJoinEntities =
                    collectionAssetJoinEntityMapper.selectList(queryWrapper);
            if (CollectionUtils.isEmpty(collectionAssetJoinEntities)) return collectionAssetsMap;

            Set<Long> assetIds = collectionAssetJoinEntities.stream()
                    .map(CollectionAssetJoinEntity::getAssetId).collect(Collectors.toSet());
            List<AssetEntity> assetEntityList = assetEntityMapper.selectBatchIds(assetIds);
            if (CollectionUtils.isEmpty(assetEntityList)) return collectionAssetsMap;

            Map<Long, AssetEntity> assetEntityMap = assetEntityList.stream()
                    .collect(Collectors.toMap(AssetEntity::getId, assetEntity -> assetEntity));

            collectionAssetJoinEntities.forEach(joinEntity -> {
                Long collectionId = joinEntity.getCollectionId();
                Long assetId = joinEntity.getAssetId();
                List<Asset> assetList = collectionAssetsMap.get(collectionId);
                AssetEntity assetEntity = assetEntityMap.get(assetId);
                Asset asset = BeanMapper.patch(assetEntity, Asset.class);
                assetList.add(asset);
            });

            return collectionAssetsMap;
        });
    }
}
