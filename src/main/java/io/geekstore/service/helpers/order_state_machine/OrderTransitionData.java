/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers.order_state_machine;

import io.geekstore.common.RequestContext;
import io.geekstore.entity.OrderEntity;
import lombok.Data;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class OrderTransitionData {
    private RequestContext ctx;
    private OrderEntity orderEntity;
}
