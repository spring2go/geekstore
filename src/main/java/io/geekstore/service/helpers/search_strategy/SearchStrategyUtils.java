/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers.search_strategy;

import io.geekstore.types.asset.Coordinate;
import io.geekstore.types.search.SearchResult;
import io.geekstore.types.search.SearchResultAsset;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.NClob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created on Jan, 2021 by @author bobo
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchStrategyUtils {
    private final ObjectMapper objectMapper;

    public SearchResult mapToSearchResult(Map<String, Object> map) {
        SearchResult searchResult = new SearchResult();

        if (map.get("product_asset_id".toUpperCase()) != null) {
            SearchResultAsset searchResultAsset = new SearchResultAsset();
            searchResultAsset.setId((Long) map.get("product_asset_id".toUpperCase()));
            searchResultAsset.setPreview((String) map.get("product_preview".toUpperCase()));
            Object clob = map.get("product_preview_focal_point".toUpperCase());
            if (clob != null) {
                String jsonString = convertClob2String((NClob) clob);
                searchResultAsset.setFocalPoint(parseFocalPoint(jsonString));
            }
            searchResult.setProductAsset(searchResultAsset);
        }

        if (map.get("product_variant_asset_id".toUpperCase()) != null) {
            SearchResultAsset searchResultAsset = new SearchResultAsset();
            searchResultAsset.setId((Long) map.get("product_variant_asset_id".toUpperCase()));
            searchResultAsset.setPreview((String) map.get("product_variant_preview".toUpperCase()));
            Object clob = map.get("product_variant_preview_focal_point".toUpperCase());
            if (clob != null) {
                String jsonString = convertClob2String((NClob) clob);
                searchResultAsset.setFocalPoint(parseFocalPoint(jsonString));
            }
            searchResult.setProductVariantAsset(searchResultAsset);
        }

        searchResult.setSku((String) map.get("sku".toUpperCase()));
        searchResult.setSlug((String) map.get("slug".toUpperCase()));
        searchResult.setEnabled((Boolean) map.get("enabled".toUpperCase()));
        searchResult.setProductVariantId((Long) map.get("product_variant_id".toUpperCase()));
        searchResult.setProductId((Long) map.get("product_id".toUpperCase()));
        searchResult.setProductName((String) map.get("product_name".toUpperCase()));
        searchResult.setProductVariantName((String) map.get("product_variant_name".toUpperCase()));
        Object clob = map.get("description".toUpperCase());
        searchResult.setDescription(convertClob2String((NClob) clob));
        searchResult.setPrice((Integer) map.get("price".toUpperCase()));


        clob = map.get("facet_ids".toUpperCase());
        String jsonString = convertClob2String((NClob) clob);
        searchResult.setFacetIds(parseLongList(jsonString));
        clob = map.get("facet_value_ids".toUpperCase());
        jsonString = convertClob2String((NClob) clob);
        if (!StringUtils.isEmpty(jsonString)) {
            searchResult.setFacetValueIds(parseLongList(jsonString));
        }
        clob = map.get("collection_ids".toUpperCase());
        jsonString = convertClob2String((NClob) clob);
        if (!StringUtils.isEmpty(jsonString)) {
            searchResult.setCollectionIds(parseLongList(jsonString));
        }

        Integer score = (Integer) map.get("score".toUpperCase());
        searchResult.setScore(score == null ? null : score.floatValue());

        return searchResult;
    }

    // 参考：
    // https://stackoverflow.com/questions/2169732/most-efficient-solution-for-reading-clob-to-string-and-string-to-clob-in-java
    private String convertClob2String(NClob clob) {
        if (clob == null) return null;
        try {
            return clob.getSubString(1, (int) clob.length());
        } catch (SQLException ex) {
            log.error("Fail to convert clob to String", ex);
        }
        return null;
    }

    private List<Long> parseLongList(String jsonString) {
        if (StringUtils.isEmpty(jsonString)) return new ArrayList<>();
        try {
            List<Long> idList = objectMapper.readValue(jsonString,
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class, Long.class));
            return idList;
        } catch (JsonProcessingException ex) {
            log.error("Fail to convert jsonString to List<Long>", ex);
        }
        return new ArrayList<>();
    }

    private Coordinate parseFocalPoint(String jsonString) {
        if (StringUtils.isEmpty(jsonString)) return null;
        try {
            return objectMapper.readValue(jsonString, Coordinate.class);
        } catch (JsonProcessingException ex) {
            log.error("Fail to convert jsonString to Coordinate", ex);
        }
        return null;
    }
}
