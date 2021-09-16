/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.order;

import io.geekstore.types.common.BooleanOperators;
import io.geekstore.types.common.DateOperators;
import io.geekstore.types.common.NumberOperators;
import io.geekstore.types.common.StringOperators;
import lombok.Data;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class OrderFilterParameter {
    private DateOperators createdAt;
    private DateOperators updatedAt;
    private StringOperators code;
    private StringOperators state;
    private BooleanOperators active;
    private NumberOperators subTotal;
    private NumberOperators total;
    private NumberOperators shipping;
}
