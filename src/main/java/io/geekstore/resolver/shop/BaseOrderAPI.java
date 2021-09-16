/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.shop;

import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.OrderEntity;
import io.geekstore.exception.InternalServerError;
import io.geekstore.service.CustomerService;
import io.geekstore.service.OrderService;
import io.geekstore.service.SessionService;
import io.geekstore.types.order.Order;
import lombok.RequiredArgsConstructor;

/**
 * Created on Dec, 2020 by @author bobo
 */

@RequiredArgsConstructor
public abstract class BaseOrderAPI {
    protected final OrderService orderService;
    protected final SessionService sessionService;
    protected final CustomerService customerService;

    protected Order getOrderFromContext(RequestContext ctx) {
        return this.getOrderFromContext(ctx, false);
    }

    protected Order getOrderFromContext(RequestContext ctx, boolean createIfNotExists) {
        if (ctx.getSession() == null) {
            throw new InternalServerError("No active session");
        }
        OrderEntity orderEntity = ctx.getSession().getActiveOrderId() != null
                ? this.orderService.findOne(ctx.getSession().getActiveOrderId())
                : null;
        if (orderEntity != null && !orderEntity.isActive()) {
            // edge case where an inactive order may not have been
            // removed from the session, i.e. the regular process was interrupted
            this.sessionService.unsetActiveOrder(ctx.getSession());
            orderEntity = null;
        }
        if (orderEntity == null) {
            if (ctx.getActiveUserId() != null) {
                orderEntity = this.orderService.getActiveOrderForUser(ctx.getActiveUserId(), false);
            }

            if (orderEntity == null && createIfNotExists) {
                orderEntity = this.orderService.create(ctx, ctx.getActiveUserId());
            }

            if (orderEntity != null) {
                this.sessionService.setActiveOrder(ctx.getSession(), orderEntity.getId());
            }
        }
        if (orderEntity == null) return null;
        return BeanMapper.map(orderEntity, Order.class);
    }
}
