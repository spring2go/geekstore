/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.ShippingMethodEntity;
import io.geekstore.service.OrderTestingService;
import io.geekstore.service.ShippingMethodService;
import io.geekstore.types.common.ConfigurableOperationDefinition;
import io.geekstore.types.common.Permission;
import io.geekstore.types.shipping.*;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class ShippingMethodQuery implements GraphQLQueryResolver {

    private final ShippingMethodService shippingMethodService;
    private final OrderTestingService orderTestingService;

    @Allow(Permission.ReadSettings)
    public ShippingMethodList shippingMethods(ShippingMethodListOptions options, DataFetchingEnvironment dfe) {
        return this.shippingMethodService.findAll(options);
    }

    @Allow(Permission.ReadSettings)
    public ShippingMethod shippingMethod(Long id, DataFetchingEnvironment dfe) {
        ShippingMethodEntity shippingMethodEntity = this.shippingMethodService.findOne(id);
        if (shippingMethodEntity == null) return null;
        return BeanMapper.map(shippingMethodEntity, ShippingMethod.class);
    }

    @Allow(Permission.ReadSettings)
    public List<ConfigurableOperationDefinition> shippingEligibilityCheckers(DataFetchingEnvironment dfe) {
        return this.shippingMethodService.getShippingEligibilityCheckers();
    }

    @Allow(Permission.ReadSettings)
    public List<ConfigurableOperationDefinition> shippingCalculators(DataFetchingEnvironment dfe) {
        return this.shippingMethodService.getShippingCalculators();
    }

    @Allow(Permission.ReadSettings)
    public TestShippingMethodResult testShippingMethod(TestShippingMethodInput input, DataFetchingEnvironment dfe) {
        return this.orderTestingService.testShippingMethod(input);
    }

    public List<ShippingMethodQuote> testEligibleShippingMethods(
            TestEligibleShippingMethodsInput input, DataFetchingEnvironment dfe) {
        return this.orderTestingService.testEligibleShippingMethods(input);
    }
}
