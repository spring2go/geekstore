/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver;

import io.geekstore.common.ApiType;
import io.geekstore.common.Constant;
import io.geekstore.common.RequestContext;
import io.geekstore.service.HistoryService;
import io.geekstore.service.OrderService;
import io.geekstore.types.address.Address;
import io.geekstore.types.common.BooleanOperators;
import io.geekstore.types.common.SortOrder;
import io.geekstore.types.customer.Customer;
import io.geekstore.types.customer.CustomerGroup;
import io.geekstore.types.history.HistoryEntryFilterParameter;
import io.geekstore.types.history.HistoryEntryList;
import io.geekstore.types.history.HistoryEntryListOptions;
import io.geekstore.types.history.HistoryEntrySortParameter;
import io.geekstore.types.order.OrderList;
import io.geekstore.types.order.OrderListOptions;
import io.geekstore.types.user.User;
import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class CustomerResolver implements GraphQLResolver<Customer> {

    private final HistoryService historyService;
    private final OrderService orderService;

    public CompletableFuture<User> getUser(Customer customer, DataFetchingEnvironment dfe) {
        final DataLoader<Long, User> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_CUSTOMER_USER);

        return dataLoader.load(customer.getId());
    }

    public CompletableFuture<List<Address>> getAddresses(Customer customer, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        if (ApiType.SHOP.equals(ctx.getApiType()) && ctx.getActiveUserId() == null) {
            // Guest customers should not be able to see this data
            CompletableFuture<List<Address>> completableFuture = new CompletableFuture<>();
            completableFuture.complete(new ArrayList<>());
            return completableFuture;
        }

        final DataLoader<Long, List<Address>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_CUSTOMER_ADDRESSES);

        return dataLoader.load(customer.getId());
    }

    public CompletableFuture<List<CustomerGroup>> getGroups(Customer customer, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        if (ApiType.SHOP.equals(ctx.getApiType())) {
            // admin only, normal customers should not be able to see this data
            CompletableFuture<List<CustomerGroup>> completableFuture = new CompletableFuture<>();
            completableFuture.complete(new ArrayList<>());
            return completableFuture;
        }

        final DataLoader<Long, List<CustomerGroup>> dataLoader = ((GraphQLContext) dfe.getContext())
                .getDataLoaderRegistry().get()
                .getDataLoader(Constant.DATA_LOADER_NAME_CUSTOMER_GROUPS);

        return dataLoader.load(customer.getId());
    }

    public HistoryEntryList getHistory(Customer customer, HistoryEntryListOptions options,
                                                          DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        if (options == null) {
            options = new HistoryEntryListOptions();
        }
        if (ApiType.SHOP.equals(ctx.getApiType())) { // show public only
            if (options.getFilter() == null) {
                options.setFilter(new HistoryEntryFilterParameter());
            }
            HistoryEntryFilterParameter filter = options.getFilter();
            BooleanOperators booleanOperators = new BooleanOperators();
            booleanOperators.setEq(true);
            filter.setIsPublic(booleanOperators);
        }

        if (options.getSort() == null) {
            HistoryEntrySortParameter sort = new HistoryEntrySortParameter();
            sort.setCreatedAt(SortOrder.ASC);
            options.setSort(sort);
        }

        return historyService.getHistoryForCustomer(customer.getId(), options);
    }

    public OrderList getOrders(Customer customer, OrderListOptions options, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        if (Objects.equals(ApiType.SHOP, ctx.getApiType()) && ctx.getActiveUserId() == null) {
            // Guest customers should not be able to see this data
            OrderList orderList = new OrderList();
            orderList.setTotalItems(0);
            return orderList;
        }
        return this.orderService.findAllWithItemsByCustomerId(customer.getId(), options);
    }
}
