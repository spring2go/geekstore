/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.promotion.utils;

import io.geekstore.entity.*;
import io.geekstore.mapper.ProductFacetValueJoinEntityMapper;
import io.geekstore.mapper.ProductVariantEntityMapper;
import io.geekstore.mapper.ProductVariantFacetValueJoinEntityMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class FacetValueChecker {
    private final ProductVariantEntityMapper productVariantEntityMapper;
    private final ProductVariantFacetValueJoinEntityMapper productVariantFacetValueJoinEntityMapper;
    private final ProductFacetValueJoinEntityMapper productFacetValueJoinEntityMapper;

    private Cache<Long, Set<Long>> variant2FacetValueIdsCache =
            CacheBuilder.newBuilder().expireAfterWrite(5000, TimeUnit.MILLISECONDS).build();

    /**
     * Checks a given {@link OrderLineEntity} against the facetValueIds and returns
     * `true` if the associated {@link ProductVariantEntity} & {@link ProductEntity} together have *all* the specified
     * {@link FacetValueEntity}s.
     */
    public boolean hasFacetValues(OrderLineEntity orderLineEntity, List<Long> facetValueIds) {
        Set<Long> allFacetValueIds =
                this.variant2FacetValueIdsCache.getIfPresent(orderLineEntity.getProductVariantId());
        if (allFacetValueIds == null) {
            ProductVariantEntity productVariantEntity =
                    productVariantEntityMapper.selectById(orderLineEntity.getProductVariantId());
            if (productVariantEntity == null) return false;

            QueryWrapper<ProductVariantFacetValueJoinEntity> productVariantFacetValueJoinEntityQueryWrapper =
                    new QueryWrapper<>();
            productVariantFacetValueJoinEntityQueryWrapper.lambda()
                    .eq(ProductVariantFacetValueJoinEntity::getProductVariantId, productVariantEntity.getId());
            List<Long> variantFacetValueIds =
                    this.productVariantFacetValueJoinEntityMapper
                            .selectList(productVariantFacetValueJoinEntityQueryWrapper)
                            .stream().map(ProductVariantFacetValueJoinEntity::getFacetValueId).collect(Collectors.toList());

            QueryWrapper<ProductFacetValueJoinEntity> productFacetValueJoinEntityQueryWrapper =
                    new QueryWrapper<>();
            productFacetValueJoinEntityQueryWrapper.lambda()
                    .eq(ProductFacetValueJoinEntity::getProductId, productVariantEntity.getProductId());
            List<Long> productFacetValueIds =
                    this.productFacetValueJoinEntityMapper.selectList(productFacetValueJoinEntityQueryWrapper)
                            .stream().map(ProductFacetValueJoinEntity::getFacetValueId).collect(Collectors.toList());

            allFacetValueIds = new HashSet<>(variantFacetValueIds);
            allFacetValueIds.addAll(productFacetValueIds);

            variant2FacetValueIdsCache.put(orderLineEntity.getProductVariantId(), allFacetValueIds);
        }

        return allFacetValueIds.containsAll(facetValueIds);
    }
}
