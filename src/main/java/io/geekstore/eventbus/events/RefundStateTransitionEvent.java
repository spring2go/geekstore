/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.eventbus.events;

import io.geekstore.common.RequestContext;
import io.geekstore.entity.OrderEntity;
import io.geekstore.entity.RefundEntity;
import io.geekstore.service.helpers.refund_state_machine.RefundState;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This event is fired whenever a {@link RefundEntity} transitions from one {@link RefundState} to another.
 *
 * Created on Dec, 2020 by @author bobo
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RefundStateTransitionEvent extends BaseEvent {
    private final RefundState fromState;
    private final RefundState toState;
    private final RequestContext ctx;
    private final RefundEntity refund;
    private final OrderEntity order;
}
