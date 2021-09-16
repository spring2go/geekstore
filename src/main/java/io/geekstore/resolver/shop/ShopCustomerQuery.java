/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.shop;

import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.CustomerEntity;
import io.geekstore.service.CustomerService;
import io.geekstore.types.customer.Customer;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class ShopCustomerQuery implements GraphQLQueryResolver {
    private final CustomerService customerService;

    public Customer activeCustomer(DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        Long userId = ctx.getActiveUserId();
        if (userId != null) {
            CustomerEntity customerEntity = this.customerService.findOneByUserId(userId);
            if (customerEntity == null) return null;
            return BeanMapper.map(customerEntity, Customer.class);
        }
        return null;
    }


}
