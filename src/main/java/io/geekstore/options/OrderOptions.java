/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.options;

import lombok.Data;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class OrderOptions {
    /**
     * The maximum number of individual items allowed in a single order. This option exists
     * to prevent excessive resource usage when dealing with very large orders. For example,
     * if an order contains a million items, then any operations on that order (modifying a quantity,
     * adding or removing an item) will require GeekStore to loop through all million items
     * to perform price calculations against active promotions. This can have a significant
     * performance impact for very large values.
     *
     * Attempting to exceed this limit will cause GeekStore to throw a {@link OrderItemsLimitError}.
     *
     * @default 999
     */
    private Integer orderItemsLimit = 999;
}
