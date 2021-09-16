/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.ProductOptionEntity;
import io.geekstore.entity.ProductOptionGroupEntity;
import io.geekstore.service.ProductOptionGroupService;
import io.geekstore.service.ProductOptionService;
import io.geekstore.types.common.Permission;
import io.geekstore.types.product.*;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class ProductOptionMutation implements GraphQLMutationResolver {

    private final ProductOptionGroupService productOptionGroupService;
    private final ProductOptionService productOptionService;

    /**
     * Craete a new ProductOptionGroup
     */
    @Allow(Permission.CreateCatalog)
    public ProductOptionGroup createProductOptionGroup(
            CreateProductOptionGroupInput input, DataFetchingEnvironment dfe) {
        ProductOptionGroupEntity productOptionGroupEntity = this.productOptionGroupService.create(input);
        return BeanMapper.map(productOptionGroupEntity, ProductOptionGroup.class);
    }

    /**
     * Update an existing ProductOptionGroup
     */
    @Allow(Permission.UpdateCatalog)
    public ProductOptionGroup updateProductOptionGroup(
            UpdateProductOptionGroupInput input, DataFetchingEnvironment dfe) {
        ProductOptionGroupEntity productOptionGroupEntity = this.productOptionGroupService.update(input);
        return BeanMapper.map(productOptionGroupEntity, ProductOptionGroup.class);
    }

    /**
     * Create a new ProductOption within a ProductOptionGroup
     */
    @Allow(Permission.CreateCatalog)
    public ProductOption createProductOption(CreateProductOptionInput input, DataFetchingEnvironment dfe) {
        ProductOptionEntity productOptionEntity = this.productOptionService.create(input);
        return BeanMapper.map(productOptionEntity, ProductOption.class);
    }

    /**
     * Update a new ProductOption within a ProductOptionGroup
     */
    @Allow(Permission.UpdateCatalog)
    public ProductOption updateProductOption(UpdateProductOptionInput input, DataFetchingEnvironment dfe) {
        ProductOptionEntity productOptionEntity = this.productOptionService.update(input);
        return BeanMapper.map(productOptionEntity, ProductOption.class);
    }
}
