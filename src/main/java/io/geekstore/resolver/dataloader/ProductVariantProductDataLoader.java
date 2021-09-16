/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.ProductEntity;
import io.geekstore.mapper.ProductEntityMapper;
import io.geekstore.types.product.Product;
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
public class ProductVariantProductDataLoader implements MappedBatchLoader<Long, Product> {

    private final ProductEntityMapper productEntityMapper;

    @Override
    public CompletionStage<Map<Long, Product>> load(Set<Long> productIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<ProductEntity> productEntities = this.productEntityMapper.selectBatchIds(productIds);
            List<Product> products = productEntities.stream()
                    .map(productEntity -> BeanMapper.map(productEntity, Product.class)).collect(Collectors.toList());
            Map<Long, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, product -> product));
            return productMap;
        });
    }
}
