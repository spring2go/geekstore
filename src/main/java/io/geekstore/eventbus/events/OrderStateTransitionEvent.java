/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.eventbus.events;

import io.geekstore.common.RequestContext;
import io.geekstore.entity.OrderEntity;
import io.geekstore.service.helpers.order_state_machine.OrderState;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This event is fired whenever an {@link io.geekstore.entity.OrderEntity} transitions from one
 * {@link io.geekstore.service.helpers.order_state_machine.OrderState} to another.
 *
 * Created on Dec, 2020 by @author bobo
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderStateTransitionEvent extends BaseEvent {
    private final OrderState fromState;
    private final OrderState toState;
    private final RequestContext ctx;
    private final OrderEntity order;
}
