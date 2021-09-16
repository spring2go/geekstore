/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.order;

import lombok.Data;

import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class CancelOrderInput {
    /**
     * The id of the order to be cancelled
     */
    private Long orderId;
    /**
     * Optionally specify which OrderLines to cancel. If not provided, all OrderLines will be cancelled.
     */
    private List<OrderLineInput> lines;
    private String reason;
}
