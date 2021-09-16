/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.ShippingMethodEntity;
import io.geekstore.service.ShippingMethodService;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.Permission;
import io.geekstore.types.shipping.CreateShippingMethodInput;
import io.geekstore.types.shipping.ShippingMethod;
import io.geekstore.types.shipping.UpdateShippingMethodInput;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class ShippingMethodMutation implements GraphQLMutationResolver {

    private final ShippingMethodService shippingMethodService;

    @Allow(Permission.CreateSettings)
    public ShippingMethod createShippingMethod(CreateShippingMethodInput input, DataFetchingEnvironment dfe) {
        ShippingMethodEntity shippingMethodEntity = this.shippingMethodService.create(input);
        return BeanMapper.map(shippingMethodEntity, ShippingMethod.class);
    }

    @Allow(Permission.UpdateSettings)
    public ShippingMethod updateShippingMethod(UpdateShippingMethodInput input, DataFetchingEnvironment dfe) {
        ShippingMethodEntity shippingMethodEntity = this.shippingMethodService.update(input);
        return BeanMapper.map(shippingMethodEntity, ShippingMethod.class);
    }

    @Allow(Permission.DeleteSettings)
    public DeletionResponse deleteShippingMethod(Long id, DataFetchingEnvironment dfe) {
        return this.shippingMethodService.softDelete(id);
    }
}
