/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers.payment_state_machine;

import io.geekstore.common.RequestContext;
import io.geekstore.entity.OrderEntity;
import io.geekstore.entity.PaymentEntity;
import lombok.Data;

/**
 * The data which is passed to the `onStateTransitionStart` function configured when constructing
 * a new `PaymentMethodHandler`
 *
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class PaymentTransitionData {
    private RequestContext ctx;
    private PaymentEntity paymentEntity;
    private OrderEntity orderEntity;
}
