/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver;

import io.geekstore.common.ApiType;
import io.geekstore.common.Constant;
import io.geekstore.common.RequestContext;
import io.geekstore.service.StockMovementService;
import io.geekstore.types.asset.Asset;
import io.geekstore.types.facet.FacetValue;
import io.geekstore.types.product.Product;
import io.geekstore.types.product.ProductOption;
import io.geekstore.types.product.ProductVariant;
import io.geekstore.types.product.StockMovementListOptions;
import io.geekstore.types.stock.StockMovementList;
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
public class ProductVariantResolver implements GraphQLResolver<ProductVariant> {

    private final StockMovementService stockMovementService;

    public CompletableFuture<Product> getProduct(ProductVariant productVariant, DataFetchingEnvironment dfe) {
        if (productVariant.getProduct() == null) {
            CompletableFuture<Product> completableFuture = new CompletableFuture<>();
            completableFuture.complete(null);
            return completableFuture;
        }

        final DataLoader<Long, Product> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_PRODUCT_VARIANT_PRODUCT);

        return dataLoader.load(productVariant.getProductId());
    }

    public CompletableFuture<Asset> getFeaturedAsset(ProductVariant productVariant, DataFetchingEnvironment dfe) {
        if (productVariant.getFeaturedAssetId() == null) {
            CompletableFuture<Asset> completableFuture = new CompletableFuture<>();
            completableFuture.complete(null);
            return completableFuture;
        }

        final DataLoader<Long, Asset> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_PRODUCT_VARIANT_FEATURED_ASSET);

        return dataLoader.load(productVariant.getFeaturedAssetId());
    }

    public CompletableFuture<List<Asset>> getAssets(ProductVariant productVariant, DataFetchingEnvironment dfe) {
        final DataLoader<Long, List<Asset>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_PRODUCT_VARIANT_ASSETS);

        return dataLoader.load(productVariant.getId());
    }

    public CompletableFuture<List<ProductOption>> getOptions(ProductVariant productVariant,
                                                             DataFetchingEnvironment dfe) {
        final DataLoader<Long, List<ProductOption>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_PRODUCT_VARIANT_OPTIONS);

        return dataLoader.load(productVariant.getId());
    }

    public CompletableFuture<List<FacetValue>> getFacetValues(ProductVariant productVariant, DataFetchingEnvironment dfe) {
        final DataLoader<Long, List<FacetValue>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_PRODUCT_VARIANT_FACET_VALUES);

        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        return dataLoader.load(productVariant.getId(), ctx);
    }

    public StockMovementList getStockMovements(
            ProductVariant productVariant, StockMovementListOptions options, DataFetchingEnvironment dfe) {

        // 只对Admin可见
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        if (ApiType.SHOP.equals(ctx.getApiType())) {
            return null;
        }

        return stockMovementService.getStockMovementsByProductVariantId(productVariant.getId(), options);
    }
}
