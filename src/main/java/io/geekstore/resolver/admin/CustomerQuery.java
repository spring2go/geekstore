/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.CustomerEntity;
import io.geekstore.service.CustomerService;
import io.geekstore.types.common.Permission;
import io.geekstore.types.customer.Customer;
import io.geekstore.types.customer.CustomerList;
import io.geekstore.types.customer.CustomerListOptions;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class CustomerQuery implements GraphQLQueryResolver {
    private final CustomerService customerService;

    @Allow(Permission.ReadCustomer)
    public CustomerList customers(CustomerListOptions options, DataFetchingEnvironment dfe) {
        return this.customerService.findAll(options);
    }

    @Allow(Permission.ReadCustomer)
    public Customer customer(Long id, DataFetchingEnvironment dfe) {
        CustomerEntity customerEntity =  this.customerService.findOne(id);
        if (customerEntity == null) return null;
        return BeanMapper.map(customerEntity, Customer.class);
    }
}
