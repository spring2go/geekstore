/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.ProductOptionEntity;
import io.geekstore.entity.ProductVariantProductOptionJoinEntity;
import io.geekstore.mapper.ProductOptionEntityMapper;
import io.geekstore.mapper.ProductVariantProductOptionJoinEntityMapper;
import io.geekstore.types.product.ProductOption;
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
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class ProductVariantOptionsDataLoader implements MappedBatchLoader<Long, List<ProductOption>> {
    private final ProductVariantProductOptionJoinEntityMapper productVariantProductOptionJoinEntityMapper;
    private final ProductOptionEntityMapper productOptionEntityMapper;

    @Override
    public CompletionStage<Map<Long, List<ProductOption>>> load(Set<Long> productVariantIds) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Long, List<ProductOption>> variantOptionsMap = new HashMap<>();
            productVariantIds.forEach(id -> variantOptionsMap.put(id, new ArrayList<>()));

            QueryWrapper<ProductVariantProductOptionJoinEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(ProductVariantProductOptionJoinEntity::getProductVariantId, productVariantIds);
            List<ProductVariantProductOptionJoinEntity> joinEntities =
                    productVariantProductOptionJoinEntityMapper.selectList(queryWrapper);
            if (CollectionUtils.isEmpty(joinEntities)) return variantOptionsMap;

            Set<Long> productOptionIds = joinEntities.stream()
                    .map(ProductVariantProductOptionJoinEntity::getProductOptionId).collect(Collectors.toSet());
            List<ProductOptionEntity> productOptionEntityList =
                    productOptionEntityMapper.selectBatchIds(productOptionIds);
            if (CollectionUtils.isEmpty(productOptionEntityList)) return variantOptionsMap;

            Map<Long, ProductOptionEntity> productOptionEntityMap = productOptionEntityList.stream()
                    .collect(Collectors.toMap(ProductOptionEntity::getId, productOptionEntity -> productOptionEntity));

            joinEntities.forEach(joinEntity -> {
                Long productVariantId = joinEntity.getProductVariantId();
                Long productOptionId = joinEntity.getProductOptionId();
                List<ProductOption> productOptionList =
                        variantOptionsMap.get(productVariantId);
                ProductOptionEntity productOptionEntity = productOptionEntityMap.get(productOptionId);
                ProductOption productOption = BeanMapper.patch(productOptionEntity, ProductOption.class);
                productOptionList.add(productOption);
            });

            return variantOptionsMap;
        });
    }
}
