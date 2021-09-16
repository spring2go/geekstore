/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver;

import io.geekstore.common.Constant;
import io.geekstore.types.payment.Payment;
import io.geekstore.types.payment.Refund;
import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class PaymentResolver implements GraphQLResolver<Payment> {
    public CompletableFuture<List<Refund>> getRefunds(Payment payment, DataFetchingEnvironment dfe) {
        final DataLoader<Long, List<Refund>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_PAYMENT_REFUNDS);

        return dataLoader.load(payment.getId());
    }
}
