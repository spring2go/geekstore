/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.ProductEntity;
import io.geekstore.entity.ProductVariantEntity;
import io.geekstore.service.ProductService;
import io.geekstore.service.ProductVariantService;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.Permission;
import io.geekstore.types.product.*;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class ProductMutation implements GraphQLMutationResolver {

    private final ProductService productService;
    private final ProductVariantService productVariantService;

    /**
     * Create a new Product
     */
    @Allow(Permission.CreateCatalog)
    public Product createProduct(CreateProductInput input, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        ProductEntity productEntity = this.productService.create(ctx, input);
        return BeanMapper.map(productEntity, Product.class);
    }

    /**
     * Update an existing Product
     */
    @Allow(Permission.UpdateCatalog)
    public Product updateProduct(UpdateProductInput input, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        ProductEntity productEntity = this.productService.update(ctx, input);
        return BeanMapper.map(productEntity, Product.class);
    }

    /**
     * Delete a Product
     */
    @Allow(Permission.DeleteCatalog)
    public DeletionResponse deleteProduct(Long id, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        DeletionResponse deletionResponse = this.productService.softDelete(ctx, id);
        return deletionResponse;
    }

    /**
     * Add an OptionGroup to a Product
     */
    @Allow(Permission.UpdateCatalog)
    public Product addOptionGroupToProduct(Long productId, Long optionGroupId, DataFetchingEnvironment dfe) {
        ProductEntity productEntity = this.productService.addOptionGroupToProduct(productId, optionGroupId);
        return BeanMapper.map(productEntity, Product.class);
    }

    /**
     * Remove an OptionGroup from a Product
     */
    @Allow(Permission.CreateCatalog)
    public Product removeOptionGroupFromProduct(Long productId, Long optionGroupId, DataFetchingEnvironment dfe) {
        ProductEntity productEntity = this.productService.removeOptionGroupFromProduct(productId, optionGroupId);
        return BeanMapper.map(productEntity, Product.class);
    }

    /**
     * Create a set of ProductVariants based on the OptionGroups assigned to the given Product
     */
    @Allow(Permission.UpdateCatalog)
    public List<ProductVariant> createProductVariants(
            List<CreateProductVariantInput> input, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        List<ProductVariantEntity> productVariantEntities = this.productVariantService.create(ctx, input);
        return productVariantEntities.stream()
                .map(variantEntity -> BeanMapper.map(variantEntity, ProductVariant.class))
                .collect(Collectors.toList());
    }

    /**
     * Update exisiting ProductVariants
     */
    @Allow(Permission.UpdateCatalog)
    public List<ProductVariant> updateProductVariants(
            List<UpdateProductVariantInput> input, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        List<ProductVariantEntity> productVariantEntities = this.productVariantService.update(ctx, input);
        return productVariantEntities.stream()
                .map(variantEntity -> BeanMapper.map(variantEntity, ProductVariant.class))
                .collect(Collectors.toList());
    }

    /**
     * Delete a ProductVariant
     */
    @Allow(Permission.DeleteCatalog)
    public DeletionResponse deleteProductVariant(Long id, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        return this.productVariantService.softDelete(ctx, id);
    }
}
