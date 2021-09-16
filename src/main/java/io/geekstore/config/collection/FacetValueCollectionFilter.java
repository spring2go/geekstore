/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.collection;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.entity.ProductFacetValueJoinEntity;
import io.geekstore.entity.ProductVariantEntity;
import io.geekstore.entity.ProductVariantFacetValueJoinEntity;
import io.geekstore.mapper.ProductFacetValueJoinEntityMapper;
import io.geekstore.mapper.ProductVariantEntityMapper;
import io.geekstore.mapper.ProductVariantFacetValueJoinEntityMapper;
import io.geekstore.types.common.ConfigArgDefinition;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Filters for ProductVariants having the given facetValueIds (including parent Product)
 *
 * Created on Nov, 2020 by @author bobo
 */
public class FacetValueCollectionFilter extends CollectionFilter {

    final static String CONFIG_NAME_FACET_VALUE_IDS = "facetValueIds";
    final static String CONFIG_NAME_CONTAINS_ANY = "containsAny";

    @Autowired
    private ProductVariantFacetValueJoinEntityMapper productVariantFacetValueJoinEntityMapper;

    @Autowired
    private ProductFacetValueJoinEntityMapper productFacetValueJoinEntityMapper;

    @Autowired
    private ProductVariantEntityMapper productVariantEntityMapper;

    @Override
    public String getCode() {
        return "facet-value-filter";
    }

    @Override
    public Map<String, ConfigArgDefinition> getArgSpec() {
        Map<String, ConfigArgDefinition> args = new HashMap<>();

        ConfigArgDefinition configArgDefinition = new ConfigArgDefinition();
        configArgDefinition.setType("ID");
        configArgDefinition.setList(true);
        configArgDefinition.getUi().put("component", "facet-value-form-input");
        args.put(CONFIG_NAME_FACET_VALUE_IDS, configArgDefinition);

        configArgDefinition = new ConfigArgDefinition();
        configArgDefinition.setType("boolean");
        args.put(CONFIG_NAME_CONTAINS_ANY, configArgDefinition);

        return args;
    }

    @Override
    public String getDescription() {
        return "Filter by FacetValues";
    }


    @Override
    public QueryWrapper<ProductVariantEntity> apply(
            ConfigArgValues configArgValues,
            QueryWrapper<ProductVariantEntity> resultQueryWrapper) {
        List<Long> facetValueIds = configArgValues.getIdList(CONFIG_NAME_FACET_VALUE_IDS);
        if (CollectionUtils.isEmpty(facetValueIds)) {
            resultQueryWrapper.apply("1 = 0");
            return resultQueryWrapper;
        }

        // 查询ProductVariant <> FacetValue关联关系
        QueryWrapper<ProductVariantFacetValueJoinEntity> variantFacetValueJoinEntityQueryWrapper
                = new QueryWrapper<>();
        variantFacetValueJoinEntityQueryWrapper.lambda()
                .in(ProductVariantFacetValueJoinEntity::getFacetValueId, facetValueIds);
        List<ProductVariantFacetValueJoinEntity> variantFacetValueJoinEntities =
                this.productVariantFacetValueJoinEntityMapper.selectList(variantFacetValueJoinEntityQueryWrapper);

        List<TempEntry> tempEntries = new ArrayList<>();
        variantFacetValueJoinEntities.forEach(joinEntity -> {
            TempEntry tempEntry = new TempEntry();
            tempEntry.setVariantId(joinEntity.getProductVariantId());
            tempEntry.setFacetValueId(joinEntity.getFacetValueId());
            tempEntries.add(tempEntry);
        });

        // 查询ProductVariant <> Product <> FacetValue关联关系
        QueryWrapper<ProductFacetValueJoinEntity> productFacetValueJoinEntityQueryWrapper
                = new QueryWrapper<>();
        productFacetValueJoinEntityQueryWrapper.lambda()
                .in(ProductFacetValueJoinEntity::getFacetValueId, facetValueIds);
        List<ProductFacetValueJoinEntity> productFacetValueJoinEntities =
                this.productFacetValueJoinEntityMapper.selectList(productFacetValueJoinEntityQueryWrapper);
        Set<Long> productIds = productFacetValueJoinEntities.stream()
                .map(ProductFacetValueJoinEntity::getProductId).collect(Collectors.toSet());

        QueryWrapper<ProductVariantEntity> productVariantEntityQueryWrapper = new QueryWrapper<>();
        productVariantEntityQueryWrapper.lambda()
                .in(ProductVariantEntity::getProductId, productIds).select()
                .select(ProductVariantEntity::getId, ProductVariantEntity::getProductId);
        List<ProductVariantEntity> productVariantEntities =
                this.productVariantEntityMapper.selectList(productVariantEntityQueryWrapper);

        productVariantEntities.forEach(variantEntity -> {
            Long productId = variantEntity.getProductId();
            for(ProductFacetValueJoinEntity joinEntity : productFacetValueJoinEntities) {
                if (productId.equals(joinEntity.getProductId())) {
                    TempEntry tempEntry = new TempEntry();
                    tempEntry.setVariantId(variantEntity.getId());
                    tempEntry.setFacetValueId(joinEntity.getFacetValueId());
                    if (!tempEntries.contains(tempEntry)) {
                        tempEntries.add(tempEntry);
                    }
                }
            }
        });

        Map<Long, Long> countedEntries = tempEntries.stream()
                .collect(Collectors.groupingBy(TempEntry::getVariantId, Collectors.counting()));

        Set<Long> resultVariantIds = new HashSet<>();
        boolean containsAny = configArgValues.getBoolean(CONFIG_NAME_CONTAINS_ANY);
        countedEntries.forEach((variantId, count) -> {
            if (!containsAny) {
                if (count >= facetValueIds.size()) {
                    resultVariantIds.add(variantId);
                }
            } else {
                resultVariantIds.add(variantId);
            }
        });

        if (CollectionUtils.isEmpty(resultVariantIds)) {
            resultQueryWrapper.apply("1 = 0");
            return resultQueryWrapper;
        }

        resultQueryWrapper.lambda().in(ProductVariantEntity::getId, resultVariantIds);
        return resultQueryWrapper;
    }

    @Data
    static class TempEntry {
        public Long variantId;
        public Long facetValueId;
    }
}
