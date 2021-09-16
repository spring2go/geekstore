/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.CustomerGroupEntity;
import io.geekstore.service.CustomerGroupService;
import io.geekstore.types.common.Permission;
import io.geekstore.types.customer.CustomerGroup;
import io.geekstore.types.customer.CustomerGroupList;
import io.geekstore.types.customer.CustomerGroupListOptions;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class CustomerGroupQuery implements GraphQLQueryResolver {

    private final CustomerGroupService customerGroupService;

    @Allow(Permission.ReadCustomer)
    public CustomerGroupList customerGroups(CustomerGroupListOptions options, DataFetchingEnvironment dfe) {
        return this.customerGroupService.findAll(options);
    }

    @Allow(Permission.ReadCustomer)
    public CustomerGroup customerGroup(Long id, DataFetchingEnvironment dfe) {
        CustomerGroupEntity customerGroupEntity = this.customerGroupService.findOne(id);
        if (customerGroupEntity == null) return null;
        return BeanMapper.map(customerGroupEntity, CustomerGroup.class);
    }
}
