/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.custom.security.Allow;
import io.geekstore.entity.OrderEntity;
import io.geekstore.service.OrderService;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.types.common.Permission;
import io.geekstore.types.order.Order;
import io.geekstore.types.order.OrderList;
import io.geekstore.types.order.OrderListOptions;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class OrderQuery implements GraphQLQueryResolver {

    private final OrderService orderService;

    /**
     * Query
     */
    @Allow(Permission.ReadOrder)
    public Order orderByAdmin(Long id, DataFetchingEnvironment dfe) {
        OrderEntity orderEntity = this.orderService.findOneWithItems(id);
        if (orderEntity == null) return null;
        return ServiceHelper.mapOrderEntityToOrder(orderEntity);
    }

    @Allow(Permission.ReadOrder)
    public OrderList orders(OrderListOptions options, DataFetchingEnvironment dfe) {
        return this.orderService.findAllWithItems(options);
    }
}
