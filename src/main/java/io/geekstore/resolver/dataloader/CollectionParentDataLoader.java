/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.CollectionEntity;
import io.geekstore.mapper.CollectionEntityMapper;
import io.geekstore.types.collection.Collection;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class CollectionParentDataLoader implements MappedBatchLoader<Long, Collection> {
    private final CollectionEntityMapper collectionEntityMapper;

    @Override
    public CompletionStage<Map<Long, Collection>> load(Set<Long> parentIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<CollectionEntity> collectionEntities =
                    this.collectionEntityMapper.selectBatchIds(parentIds);
            List<Collection> collections = collectionEntities.stream()
                    .map(collectionEntity -> BeanMapper.map(collectionEntity, Collection.class))
                    .collect(Collectors.toList());
            Map<Long, Collection> collectionMap =
                    collections.stream().collect(Collectors.toMap(Collection::getId, c -> c));
            return collectionMap;
        });
    }
}
