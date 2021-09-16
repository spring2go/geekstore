/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver;

import io.geekstore.service.CustomerGroupService;
import io.geekstore.types.customer.CustomerGroup;
import io.geekstore.types.customer.CustomerList;
import io.geekstore.types.customer.CustomerListOptions;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class CustomerGroupResolver implements GraphQLResolver<CustomerGroup> {

    private final CustomerGroupService customerGroupService;

    public CustomerList getCustomers(
            CustomerGroup customerGroup, CustomerListOptions options, DataFetchingEnvironment dfe) {
        return this.customerGroupService.getGroupCustomers(customerGroup.getId(), options);
    }
}
