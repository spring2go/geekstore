/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers.search_strategy;

import io.geekstore.entity.SearchIndexItemEntity;
import io.geekstore.mapper.SearchIndexItemEntityMapper;
import io.geekstore.types.common.LogicalOperator;
import io.geekstore.types.common.SearchInput;
import io.geekstore.types.common.SearchResultSortParameter;
import io.geekstore.types.common.SortOrder;
import io.geekstore.types.search.SearchResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A rather native search for H2/MySQL Database. Rather than proper full-text searching,
 * it uses a weighted `LIKE '%term%'` operator instead.
 *
 * Created on Jan, 2021 by @author bobo
 */
public class DbSearchStrategy implements SearchStrategy {
    @Autowired
    private SearchIndexItemEntityMapper searchIndexItemEntityMapper;
    @Autowired
    private SearchStrategyUtils searchStrategyUtils;
    private final Integer minTermLength = 2;

    @Override
    public List<SearchResult> getSearchResults(SearchInput input, boolean enabledOnly) {
        Integer pageSize = input.getPageSize() == null ? 25 : input.getPageSize();
        Integer currentPage = input.getCurrentPage() == null ? 1 : input.getCurrentPage();

        SearchResultSortParameter sort = input.getSort();

        List<String> selectColumns = new ArrayList<>();
        selectColumns.addAll(Arrays.asList(
                "price",
                "product_asset_id",
                "product_preview",
                "product_preview_focal_point",
                "product_variant_asset_id",
                "product_variant_preview",
                "product_variant_preview_focal_point",
                "sku",
                "slug",
                "enabled",
                "product_variant_id",
                "product_id",
                "product_name",
                "product_variant_name",
                "description",
                "facet_ids",
                "facet_value_ids",
                "collection_ids"
        ));

        QueryWrapper<SearchIndexItemEntity> queryWrapper = new QueryWrapper<>();
        List<String> newColumns = applyTermAndFilters(queryWrapper, input);
        selectColumns.addAll(newColumns);

        if (!StringUtils.isEmpty(input.getTerm()) && input.getTerm().length() > this.minTermLength) {
            queryWrapper.orderByDesc("score");
        }

        if (sort != null) {
            if (sort.getName() != null) {
                if (SortOrder.ASC == sort.getName()) {
                    queryWrapper.lambda().orderByAsc(SearchIndexItemEntity::getProductName);
                } else {
                    queryWrapper.lambda().orderByDesc(SearchIndexItemEntity::getProductName);
                }
            }
            if (sort.getPrice() != null) {
                if (SortOrder.ASC == sort.getPrice()) {
                    queryWrapper.lambda().orderByAsc(SearchIndexItemEntity::getPrice);
                } else {
                    queryWrapper.lambda().orderByDesc(SearchIndexItemEntity::getPrice);
                }
            }
        } else {
            queryWrapper.lambda().orderByAsc(SearchIndexItemEntity::getProductVariantId);
        }

        if (enabledOnly) {
            queryWrapper.lambda().eq(SearchIndexItemEntity::isEnabled, true);
        }

        queryWrapper.select(selectColumns.toArray(new String[0]));

        IPage<Map<String, Object>> page = new Page<>(currentPage, pageSize);
        IPage<Map<String, Object>> mapsPage =
                this.searchIndexItemEntityMapper.selectMapsPage(page, queryWrapper);

        return mapsPage.getRecords().stream()
                .map(r -> searchStrategyUtils.mapToSearchResult(r))
                .collect(Collectors.toList());
    }

    @Override
    public Integer getTotalCount(SearchInput input, boolean enabledOnly) {
        QueryWrapper<SearchIndexItemEntity> queryWrapper = new QueryWrapper<>();
        applyTermAndFilters(queryWrapper, input);

        if (enabledOnly) {
            queryWrapper.lambda().eq(SearchIndexItemEntity::isEnabled, true);
        }

        return this.searchIndexItemEntityMapper.selectCount(queryWrapper);
    }

    @Override
    public Map<Long, Integer> getFacetValueIds(SearchInput input, boolean enabledOnly) {
        QueryWrapper<SearchIndexItemEntity> queryWrapper = new QueryWrapper<>();
        applyTermAndFilters(queryWrapper, input);
        queryWrapper.lambda()
                .select(SearchIndexItemEntity::getProductVariantId)
                .select(SearchIndexItemEntity::getFacetValueIds);

        if (enabledOnly) {
            queryWrapper.lambda().eq(SearchIndexItemEntity::isEnabled, true);
        }

        List<SearchIndexItemEntity> indexItems = this.searchIndexItemEntityMapper.selectList(queryWrapper);

        // 参考：
        // https://stackoverflow.com/questions/81346/most-efficient-way-to-increment-a-map-value-in-java
        Map<Long, Integer> result = new HashMap<>();
        for(SearchIndexItemEntity item : indexItems) {
            for (Long facetValueId : item.getFacetValueIds()) {
                result.merge(facetValueId, 1, Integer::sum);
            }
        }

        return result;
    }

    private List<String> applyTermAndFilters(
            QueryWrapper<SearchIndexItemEntity> queryWrapper, SearchInput input) {
        queryWrapper.apply("1 = 1");

        String term = input.getTerm() == null ? null : input.getTerm().trim();

        List<String> selectColumns = new ArrayList<>();
        if (!StringUtils.isEmpty(term) && term.length() > this.minTermLength) {
            String scoreColumn = "case " +
                            "when sku like '%" + term + "%' then 10 " +
                            "when product_name like '%" + term + "%' then 3 " +
                            "when product_variant_name like '%" + term + "%' then 2 " +
                            "when description like '%" + term + "%' then 1 " +
                            "else 0 " +
                            "end as score";
            selectColumns.add(scoreColumn);

            queryWrapper.lambda().like(SearchIndexItemEntity::getSku, term)
                    .or().like(SearchIndexItemEntity::getProductName, term)
                    .or().like(SearchIndexItemEntity::getProductVariantName, term)
                    .or().like(SearchIndexItemEntity::getDescription, term);
        }

        if (!CollectionUtils.isEmpty(input.getFacetValueIds())) {
            String likeColumn = "regexp_replace(facet_value_ids, '[\\[|\\]]', ',')";
            queryWrapper.and(w -> {
                for(Long id: input.getFacetValueIds()) {
                    String likeTerm = "," + id + ",";
                    if (Objects.equals(input.getFacetValueOperator(), LogicalOperator.AND)) {
                        w.like(likeColumn, likeTerm);
                    } else {
                        w.or().like(likeColumn, likeTerm);
                    }
                }
            });

        }

        if (input.getCollectionId() != null) {
            queryWrapper.like("regexp_replace(collection_ids, '[\\[|\\]]', ',')", "," + input.getCollectionId() + ",");
        }
        if (!StringUtils.isEmpty(input.getCollectionSlug())) {
            queryWrapper.like("collection_slugs",
                    "\"" + input.getCollectionSlug().trim() + "\"");
        }

        return selectColumns;
    }
}
