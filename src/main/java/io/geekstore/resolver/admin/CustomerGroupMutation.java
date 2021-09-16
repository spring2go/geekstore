/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.CustomerGroupEntity;
import io.geekstore.service.CustomerGroupService;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.customer.CreateCustomerGroupInput;
import io.geekstore.types.customer.CustomerGroup;
import io.geekstore.types.customer.UpdateCustomerGroupInput;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class CustomerGroupMutation implements GraphQLMutationResolver {

    private final CustomerGroupService customerGroupService;

    /**
     * Create a new CustomerGroup
     */
    public CustomerGroup createCustomerGroup(CreateCustomerGroupInput input, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        CustomerGroupEntity customerGroupEntity = this.customerGroupService.create(ctx, input);
        return BeanMapper.map(customerGroupEntity, CustomerGroup.class);
    }

    /**
     * Update an existing CustomerGroup
     */
    public CustomerGroup updateCustomerGroup(UpdateCustomerGroupInput input, DataFetchingEnvironment dfe) {
        CustomerGroupEntity customerGroupEntity = this.customerGroupService.update(input);
        return BeanMapper.map(customerGroupEntity, CustomerGroup.class);
    }

    /**
     * Delete a CustomerGroup
     */
    public DeletionResponse deleteCustomerGroup(Long id, DataFetchingEnvironment dfe) {
        return this.customerGroupService.delete(id);
    }

    /**
     * Add Customers to a CustomerGroup
     */
    public CustomerGroup addCustomersToGroup(
            Long customerGroupId, List<Long> customerIds, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        CustomerGroupEntity customerGroupEntity =
                this.customerGroupService.addCustomersToGroup(ctx, customerGroupId, customerIds);
        return BeanMapper.map(customerGroupEntity, CustomerGroup.class);
    }

    /**
     * Remove Customers from a CustomerGroup
     */
    public CustomerGroup removeCustomersFromGroup(
            Long customerGroupId, List<Long> customerIds, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        CustomerGroupEntity customerGroupEntity =
                this.customerGroupService.removeCustomersFromGroup(ctx, customerGroupId, customerIds);
        return BeanMapper.map(customerGroupEntity, CustomerGroup.class);
    }
}
