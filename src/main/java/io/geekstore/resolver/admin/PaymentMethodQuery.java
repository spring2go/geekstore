/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.PaymentMethodEntity;
import io.geekstore.service.PaymentMethodService;
import io.geekstore.types.common.Permission;
import io.geekstore.types.payment.PaymentMethod;
import io.geekstore.types.payment.PaymentMethodList;
import io.geekstore.types.payment.PaymentMethodListOptions;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class PaymentMethodQuery implements GraphQLQueryResolver {

    private final PaymentMethodService paymentMethodService;

    @Allow(Permission.ReadSettings)
    public PaymentMethod paymentMethod(Long id, DataFetchingEnvironment dfe)  {
        PaymentMethodEntity paymentMethodEntity =  paymentMethodService.findOne(id);
        if (paymentMethodEntity == null) return null;
        return BeanMapper.map(paymentMethodEntity, PaymentMethod.class);
    }

    @Allow(Permission.ReadSettings)
    public PaymentMethodList paymentMethods(PaymentMethodListOptions options, DataFetchingEnvironment dfe) {
        return paymentMethodService.findAll(options);
    }
}
