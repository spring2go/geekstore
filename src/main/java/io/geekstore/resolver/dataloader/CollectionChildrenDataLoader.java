/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.CollectionEntity;
import io.geekstore.mapper.CollectionEntityMapper;
import io.geekstore.types.collection.Collection;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dataloader.MappedBatchLoader;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
@RequiredArgsConstructor
public class CollectionChildrenDataLoader implements MappedBatchLoader<Long, List<Collection>> {
    private final CollectionEntityMapper collectionEntityMapper;

    @Override
    public CompletionStage<Map<Long, List<Collection>>> load(Set<Long> parentIds) {
        return CompletableFuture.supplyAsync(() -> {
            QueryWrapper<CollectionEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(CollectionEntity::getParentId, parentIds);
            List<CollectionEntity> collectionEntities = collectionEntityMapper
                    .selectList(queryWrapper);

            if (collectionEntities.size() == 0) {
                Map<Long, List<Collection>> emptyMap = new HashMap<>();
                parentIds.forEach(id -> emptyMap.put(id, new ArrayList<>()));
                return emptyMap;
            }

            Map<Long, List<Collection>> groupByCollectionId = collectionEntities.stream()
                    .collect(Collectors.groupingBy(CollectionEntity::getParentId,
                            Collectors.mapping(collectionEntity -> BeanMapper.map(collectionEntity, Collection.class),
                                    Collectors.toList()
                            )));

            return groupByCollectionId;
        });
    }
}
