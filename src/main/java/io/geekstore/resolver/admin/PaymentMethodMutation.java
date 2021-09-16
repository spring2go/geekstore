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
import io.geekstore.types.payment.UpdatePaymentMethodInput;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class PaymentMethodMutation implements GraphQLMutationResolver {

    private final PaymentMethodService paymentMethodService;

    /**
     * Update an existing PaymentMethod
     */
    @Allow(Permission.UpdateSettings)
    public PaymentMethod updatePaymentMethod(UpdatePaymentMethodInput input, DataFetchingEnvironment dfe) {
        PaymentMethodEntity paymentMethodEntity = this.paymentMethodService.update(input);
        return BeanMapper.map(paymentMethodEntity, PaymentMethod.class);
    }
}
