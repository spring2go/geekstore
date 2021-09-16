/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.promotion;

import io.geekstore.types.common.BooleanOperators;
import io.geekstore.types.common.DateOperators;
import io.geekstore.types.common.NumberOperators;
import io.geekstore.types.common.StringOperators;
import lombok.Data;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class PromotionFilterParameter {
    private DateOperators createdAt;
    private DateOperators updatedAt;
    private DateOperators startsAt;
    private DateOperators endsAt;
    private StringOperators couponCode;
    private NumberOperators perCustomerUsageLimit;
    private StringOperators name;
    private BooleanOperators enabled;
}
