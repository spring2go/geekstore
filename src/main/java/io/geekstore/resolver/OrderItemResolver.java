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

import java.util.concurrent.CompletableFuture;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
public class OrderItemResolver implements GraphQLResolver<OrderItem> {
    public CompletableFuture<Fulfillment> getFulfillment(OrderItem orderItem, DataFetchingEnvironment dfe) {
        if (orderItem.getFulfillmentId() == null) {
            CompletableFuture<Fulfillment> completableFuture = new CompletableFuture<>();
            completableFuture.complete(null);
            return completableFuture;
        }

        final DataLoader<Long, Fulfillment> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_ORDER_ITEM_FULFILLMENT);

        return dataLoader.load(orderItem.getFulfillmentId());
    }
}
