/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.FacetEntity;
import io.geekstore.entity.FacetValueEntity;
import io.geekstore.eventbus.events.ReIndexEvent;
import io.geekstore.mapper.FacetEntityMapper;
import io.geekstore.mapper.FacetValueEntityMapper;
import io.geekstore.service.helpers.search_strategy.SearchStrategy;
import io.geekstore.types.common.SearchInput;
import io.geekstore.types.facet.FacetValue;
import io.geekstore.types.search.FacetValueResult;
import io.geekstore.types.search.SearchResponse;
import io.geekstore.types.search.SearchResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.eventbus.EventBus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Search for supported database. See the various SearchStrategy implementations for db-specific code.
 *
 * Created on Jan, 2021 by @author bobo
 */
@Service
@RequiredArgsConstructor
public class SearchService {
    private final SearchStrategy searchStrategy;
    private final FacetValueEntityMapper facetValueEntityMapper;
    private final FacetEntityMapper facetEntityMapper;
    private final EventBus eventBus;

    public SearchResponse search(SearchInput input) {
        return this.search(input, false);
    }

    /**
     * Perform a search according to the provided input arguments.
     */
    public SearchResponse search(SearchInput input, boolean enabledOnly) {
        List<SearchResult> items = this.searchStrategy.getSearchResults(input, enabledOnly);
        Integer totalItems = this.searchStrategy.getTotalCount(input, enabledOnly);

        SearchResponse response = new SearchResponse();
        response.setItems(items);
        response.setTotalItems(totalItems);

        return response;
    }

    public List<FacetValueResult> facetValues(SearchInput input, boolean publicOnly) {
        return this.facetValues(input, false, publicOnly);
    }

    /**
     * Return a list of all FacetValues which appear in the result set.
     */
    public List<FacetValueResult> facetValues(SearchInput input, boolean enabledOnly, boolean publicOnly) {
        Map<Long, Integer> facetValueIdsMap  = this.searchStrategy.getFacetValueIds(input, enabledOnly);
        List<FacetValueEntity> facetValueEntities =
                this.facetValueEntityMapper.selectBatchIds(facetValueIdsMap.keySet());
        if (CollectionUtils.isEmpty(facetValueEntities)) return new ArrayList<>();

        if (publicOnly) {
            Set<Long> facetIds = facetValueEntities.stream()
                    .map(FacetValueEntity::getFacetId).collect(Collectors.toSet());
            QueryWrapper<FacetEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(FacetEntity::isPrivateOnly, true).in(FacetEntity::getId, facetIds);

            List<FacetEntity> facetEntities = this.facetEntityMapper.selectList(null);

            Set<Long> privateOnlyFacetIds = this.facetEntityMapper.selectList(queryWrapper)
                    .stream().map(FacetEntity::getId).collect(Collectors.toSet());

            if (!CollectionUtils.isEmpty(privateOnlyFacetIds)) {
                // 过滤掉Facet是private的FacetValue
                facetValueEntities = facetValueEntities.stream()
                        .filter(facetValueEntity -> !privateOnlyFacetIds.contains(facetValueEntity.getFacetId()))
                        .collect(Collectors.toList());
            }
        }

        List<FacetValueResult> facetValueResults = facetValueEntities.stream().map(facetValueEntity -> {
            FacetValue facetValue = BeanMapper.map(facetValueEntity, FacetValue.class);
            Integer count = facetValueIdsMap.get(facetValue.getId());
            FacetValueResult result = new FacetValueResult();
            result.setFacetValue(facetValue);
            result.setCount(count);
            return result;
        }).collect(Collectors.toList());

        return facetValueResults;
    }

    public Boolean reindex() {
        this.eventBus.post(new ReIndexEvent());
        return true;
    }

}
