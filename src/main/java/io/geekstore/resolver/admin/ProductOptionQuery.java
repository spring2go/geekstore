/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.ProductOptionGroupEntity;
import io.geekstore.service.ProductOptionGroupService;
import io.geekstore.types.common.Permission;
import io.geekstore.types.product.ProductOptionGroup;
import graphql.kickstart.tools.GraphQLQueryResolver;
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
public class ProductOptionQuery implements GraphQLQueryResolver {
    private final ProductOptionGroupService productOptionGroupService;

    @Allow(Permission.ReadCatalog)
    public List<ProductOptionGroup> productOptionGroups(String filterTerm, DataFetchingEnvironment dfe) {
        List<ProductOptionGroupEntity> productOptionGroupEntityList = productOptionGroupService.findAll(filterTerm);
        return productOptionGroupEntityList.stream()
                .map(productOptionGroupEntity -> BeanMapper.map(productOptionGroupEntity, ProductOptionGroup.class))
                .collect(Collectors.toList());
    }

    @Allow(Permission.ReadCatalog)
    public ProductOptionGroup productOptionGroup(Long id, DataFetchingEnvironment dfe) {
        ProductOptionGroupEntity productOptionGroupEntity = this.productOptionGroupService.findOne(id);
        if (productOptionGroupEntity == null) return null;
        return BeanMapper.map(productOptionGroupEntity, ProductOptionGroup.class);
    }
}
