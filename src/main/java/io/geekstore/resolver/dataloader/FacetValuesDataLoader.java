/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.FacetValueEntity;
import io.geekstore.mapper.FacetValueEntityMapper;
import io.geekstore.types.facet.FacetValue;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.dataloader.MappedBatchLoader;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class FacetValuesDataLoader implements MappedBatchLoader<Long, List<FacetValue>> {
    private final FacetValueEntityMapper facetValueEntityMapper;

    public FacetValuesDataLoader(FacetValueEntityMapper facetValueEntityMapper) {
        this.facetValueEntityMapper = facetValueEntityMapper;
    }

    @Override
    public CompletionStage<Map<Long, List<FacetValue>>> load(Set<Long> facetIds) {
        return CompletableFuture.supplyAsync(() -> {
            QueryWrapper<FacetValueEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(FacetValueEntity::getFacetId, facetIds);
            List<FacetValueEntity> facetValueEntityList = this.facetValueEntityMapper.selectList(queryWrapper);

            if (facetValueEntityList.size() == 0) {
                Map<Long, List<FacetValue>> emptyMap = new HashMap<>();
                facetIds.forEach(id -> emptyMap.put(id, new ArrayList<>()));
                return emptyMap;
            }

            Map<Long, List<FacetValue>> groupByFacetId = facetValueEntityList.stream()
                    .collect(Collectors.groupingBy(FacetValueEntity::getFacetId,
                            Collectors.mapping(facetValueEntity -> BeanMapper.map(facetValueEntity, FacetValue.class),
                                    Collectors.toList()
                            )));

            return groupByFacetId;
        });
    }
}
