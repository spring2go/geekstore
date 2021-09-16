/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.ProductEntity;
import io.geekstore.entity.ProductVariantEntity;
import io.geekstore.exception.UserInputException;
import io.geekstore.service.ProductService;
import io.geekstore.service.ProductVariantService;
import io.geekstore.types.common.Permission;
import io.geekstore.types.product.Product;
import io.geekstore.types.product.ProductList;
import io.geekstore.types.product.ProductListOptions;
import io.geekstore.types.product.ProductVariant;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class ProductQuery implements GraphQLQueryResolver {

    private final ProductService productService;
    private final ProductVariantService productVariantService;

    @Allow(Permission.ReadCatalog)
    public ProductList adminProducts(ProductListOptions options, DataFetchingEnvironment dfe) {
        return this.productService.findAll(options);
    }

    /**
     * Get a Product either by id or slug. If neither id nor slug is specified, an error will result.
     */
    @Allow(Permission.ReadCatalog)
    public Product adminProduct(Long id, String slug, DataFetchingEnvironment dfe) {
        if (id != null) {
            ProductEntity productEntity = this.productService.findOne(id);
            if (productEntity == null) return null;
            if (slug != null && !slug.equals(productEntity.getSlug())) {
                throw new UserInputException("The provided id and slug refer to different Products");
            }
            return BeanMapper.map(productEntity, Product.class);
        } else if (slug != null) {
            ProductEntity productEntity = this.productService.findOneBySlug(slug);
            if (productEntity == null) return null;
            return BeanMapper.map(productEntity, Product.class);
        } else {
            throw new UserInputException("Either the Product id or slug must be provided");
        }
    }

    /**
     * Get a ProductVariant by id
     */
    @Allow(Permission.ReadCatalog)
    public ProductVariant productVariant(Long id, DataFetchingEnvironment dfe) {
        ProductVariantEntity productVariantEntity = productVariantService.findOne(id);
        if (productVariantEntity == null) return null;
        return BeanMapper.map(productVariantEntity, ProductVariant.class);
    }
}
