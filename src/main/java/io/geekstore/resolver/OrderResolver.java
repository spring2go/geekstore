/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver;

import io.geekstore.common.ApiType;
import io.geekstore.common.Constant;
import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.FulfillmentEntity;
import io.geekstore.entity.OrderEntity;
import io.geekstore.service.HistoryService;
import io.geekstore.service.OrderService;
import io.geekstore.service.helpers.order_state_machine.OrderState;
import io.geekstore.types.common.SortOrder;
import io.geekstore.types.customer.Customer;
import io.geekstore.types.history.HistoryEntryList;
import io.geekstore.types.history.HistoryEntryListOptions;
import io.geekstore.types.history.HistoryEntrySortParameter;
import io.geekstore.types.order.Fulfillment;
import io.geekstore.types.order.Order;
import io.geekstore.types.order.OrderLine;
import io.geekstore.types.payment.Payment;
import io.geekstore.types.promotion.Promotion;
import io.geekstore.types.shipping.ShippingMethod;
import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class OrderResolver implements GraphQLResolver<Order> {

    private final OrderService orderService;
    private final HistoryService historyService;

    public CompletableFuture<Customer> getCustomer(Order order, DataFetchingEnvironment dfe) {
        if (order.getCustomerId() == null) {
            CompletableFuture<Customer> completableFuture = new CompletableFuture<>();
            completableFuture.complete(null);
            return completableFuture;
        }

        final DataLoader<Long, Customer> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_ORDER_CUSTOMER);

        return dataLoader.load(order.getCustomerId());
    }

    public CompletableFuture<List<Promotion>> getPromotions(Order order, DataFetchingEnvironment dfe) {
        final DataLoader<Long, List<Promotion>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_ORDER_PROMOTIONS);

        return dataLoader.load(order.getId());
    }

    public CompletableFuture<List<Payment>> getPayments(Order order, DataFetchingEnvironment dfe) {
        final DataLoader<Long, List<Payment>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_ORDER_PAYMENTS);

        return dataLoader.load(order.getId());
    }

    public List<Fulfillment> getFulfillments(Order order, DataFetchingEnvironment dfe) {
        OrderEntity orderEntity = this.orderService.findOne(order.getId());
        List<FulfillmentEntity> fulfillmentEntities = this.orderService.getOrderFulfillments(orderEntity);
        return fulfillmentEntities.stream()
                .map(fulfillmentEntity -> BeanMapper.map(fulfillmentEntity, Fulfillment.class))
                .collect(Collectors.toList());
    }

    public CompletableFuture<ShippingMethod> getShippingMethod(Order order, DataFetchingEnvironment dfe) {
        if (order.getShippingMethodId() == null) {
            CompletableFuture<ShippingMethod> completableFuture = new CompletableFuture<>();
            completableFuture.complete(null);
            return completableFuture;
        }

        final DataLoader<Long, ShippingMethod> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_ORDER_SHIPPING_METHOD);

        return dataLoader.load(order.getShippingMethodId());
    }

    public HistoryEntryList getHistory(Order order, HistoryEntryListOptions options, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);

        if (options == null) {
            options = new HistoryEntryListOptions();
        }
        if (options.getSort() == null) {
            HistoryEntrySortParameter sort = new HistoryEntrySortParameter();
            sort.setCreatedAt(SortOrder.ASC);
            options.setSort(sort);
        }

        return this.historyService.getHistoryForOrder(
                order.getId(), ctx.getApiType() == ApiType.SHOP, options);
    }

    public List<String> getNextStates(Order order, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);

        if (ctx.getApiType() == ApiType.SHOP) return null; // Admin Only API

        return this.orderService.getNextOrderStates(order.getId()).stream()
                .map(OrderState::name).collect(Collectors.toList());
    }

    public CompletableFuture<List<OrderLine>> getLines(Order order, DataFetchingEnvironment dfe) {
        final DataLoader<Long, List<OrderLine>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_ORDER_LINES);

        return dataLoader.load(order.getId());
    }
}
