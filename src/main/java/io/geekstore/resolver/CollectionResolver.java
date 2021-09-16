/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver;

import io.geekstore.common.ApiType;
import io.geekstore.common.Constant;
import io.geekstore.common.RequestContext;
import io.geekstore.service.CollectionService;
import io.geekstore.service.ProductVariantService;
import io.geekstore.types.asset.Asset;
import io.geekstore.types.collection.Collection;
import io.geekstore.types.collection.CollectionBreadcrumb;
import io.geekstore.types.common.BooleanOperators;
import io.geekstore.types.product.ProductVariantFilterParameter;
import io.geekstore.types.product.ProductVariantList;
import io.geekstore.types.product.ProductVariantListOptions;
import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class CollectionResolver implements GraphQLResolver<Collection> {

    private final CollectionService collectionService;
    private final ProductVariantService productVariantService;

    public CompletableFuture<Asset> getFeaturedAsset(Collection collection, DataFetchingEnvironment dfe) {
        if (collection.getFeaturedAssetId() == null) {
            CompletableFuture<Asset> completableFuture = new CompletableFuture<>();
            completableFuture.complete(null);
            return completableFuture;
        }

        final DataLoader<Long, Asset> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_COLLECTION_FEATURED_ASSET);

        return dataLoader.load(collection.getFeaturedAssetId());
    }

    public CompletableFuture<List<Asset>> getAssets(Collection collection, DataFetchingEnvironment dfe) {
        final DataLoader<Long, List<Asset>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_COLLECTION_ASSETS);

        return dataLoader.load(collection.getId());
    }

    public CompletableFuture<Collection> getParent(Collection collection, DataFetchingEnvironment dfe) {
        if (collection.getParentId() == null) {
            CompletableFuture<Collection> completableFuture = new CompletableFuture<>();
            completableFuture.complete(null);
            return completableFuture;
        }

        final DataLoader<Long, Collection> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_COLLECTION_PARENT);

        return dataLoader.load(collection.getParentId());
    }

    public CompletableFuture<List<Collection>> getChildren(
            Collection collection, DataFetchingEnvironment dfe) {
        final DataLoader<Long, List<Collection>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_COLLECTION_CHILDREN);

        return dataLoader.load(collection.getId());
    }

    public List<CollectionBreadcrumb> getBreadcrumbs(Collection collection, DataFetchingEnvironment dfe) {
        return collectionService.getBreadcrumbs(collection.getId());
    }

    public ProductVariantList getProductVariants(
            Collection collection, ProductVariantListOptions options, DataFetchingEnvironment dfe) {

        // enabled = true才对SHOP可见
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        if (ApiType.SHOP.equals(ctx.getApiType())) {
            if (options == null) {
                options = new ProductVariantListOptions();
            }
            if (options.getFilter() == null) {
                options.setFilter(new ProductVariantFilterParameter());
            }
            ProductVariantFilterParameter filter = options.getFilter();
            BooleanOperators booleanOperators = new BooleanOperators();
            booleanOperators.setEq(true);
            filter.setEnabled(booleanOperators);
        }

        return this.productVariantService.getVariantsByCollectionId(collection.getId(), options);
    }
}
