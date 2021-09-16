/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers.refund_state_machine;

import io.geekstore.common.RequestContext;
import io.geekstore.entity.OrderEntity;
import io.geekstore.entity.RefundEntity;
import lombok.Data;

/**
 * The data which is passed to the state transition handlers of the RefundStateMachine.
 *
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class RefundTransitionData {
    private RequestContext ctx;
    private OrderEntity orderEntity;
    private RefundEntity refundEntity;
}
