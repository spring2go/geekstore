/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver;

import io.geekstore.common.ApiType;
import io.geekstore.common.Constant;
import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.CollectionEntity;
import io.geekstore.service.CollectionService;
import io.geekstore.types.asset.Asset;
import io.geekstore.types.collection.Collection;
import io.geekstore.types.facet.FacetValue;
import io.geekstore.types.product.Product;
import io.geekstore.types.product.ProductOptionGroup;
import io.geekstore.types.product.ProductVariant;
import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class ProductResolver implements GraphQLResolver<Product> {

    private final CollectionService collectionService;

    public CompletableFuture<Asset> getFeaturedAsset(Product product, DataFetchingEnvironment dfe) {
        if (product.getFeaturedAssetId() == null) {
            CompletableFuture<Asset> completableFuture = new CompletableFuture<>();
            completableFuture.complete(null);
            return completableFuture;
        }

        final DataLoader<Long, Asset> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_PRODUCT_FEATURED_ASSET);

        return dataLoader.load(product.getFeaturedAssetId());
    }

    public CompletableFuture<List<Asset>> getAssets(Product product, DataFetchingEnvironment dfe) {
        final DataLoader<Long, List<Asset>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_PRODUCT_ASSETS);

        return dataLoader.load(product.getId());
    }

    public CompletableFuture<List<ProductVariant>> getVariants(Product product, DataFetchingEnvironment dfe) {
        final DataLoader<Long, List<ProductVariant>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_PRODUCT_VARIANTS);

        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);

        return dataLoader.load(product.getId(), ctx);
    }

    public CompletableFuture<List<ProductOptionGroup>> getOptionGroups(Product product, DataFetchingEnvironment dfe) {
        final DataLoader<Long, List<ProductOptionGroup>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_PRODUCT_OPTION_GROUPS);

        return dataLoader.load(product.getId());
    }

    public CompletableFuture<List<FacetValue>> getFacetValues(Product product, DataFetchingEnvironment dfe) {
        final DataLoader<Long, List<FacetValue>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_PRODUCT_FACET_VALUES);

        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        return dataLoader.load(product.getId(), ctx);
    }

    public List<Collection> getCollections(Product product, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        boolean isPubic = ApiType.SHOP.equals(ctx.getApiType());
        List<CollectionEntity> collectionEntities =
                this.collectionService.getCollectionsByProductId(product.getId(), isPubic);
        return collectionEntities.stream()
                .map(collectionEntity -> BeanMapper.map(collectionEntity, Collection.class))
                .collect(Collectors.toList());

    }
}
