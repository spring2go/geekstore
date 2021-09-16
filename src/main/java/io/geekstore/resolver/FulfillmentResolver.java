/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver;

import io.geekstore.common.Constant;
import io.geekstore.types.order.Fulfillment;
import io.geekstore.types.order.OrderItem;
import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
public class FulfillmentResolver implements GraphQLResolver<Fulfillment> {
    public CompletableFuture<List<OrderItem>> getOrderItems(Fulfillment fulfillment, DataFetchingEnvironment dfe) {
        final DataLoader<Long, List<OrderItem>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_FULFILLMENT_ORDER_ITEMS);

        return dataLoader.load(fulfillment.getId());
    }
}
