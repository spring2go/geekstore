/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver;

import io.geekstore.service.PaymentMethodService;
import io.geekstore.types.common.ConfigurableOperationDefinition;
import io.geekstore.types.payment.PaymentMethod;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class PaymentMethodResolver implements GraphQLResolver<PaymentMethod> {

    private final PaymentMethodService paymentMethodService;

    public ConfigurableOperationDefinition getDefinition(PaymentMethod paymentMethod, DataFetchingEnvironment dfe) {
        return this.paymentMethodService.getPaymentMethodHandler(paymentMethod.getCode()).toGraphQLType();
    }
}
