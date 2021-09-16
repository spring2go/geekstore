/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.promotion;

import io.geekstore.types.common.SortOrder;
import lombok.Data;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class PromotionSortParameter {
    private SortOrder id;
    private SortOrder createdAt;
    private SortOrder updatedAt;
    private SortOrder startsAt;
    private SortOrder endsAt;
    private SortOrder couponCode;
    private SortOrder perCustomerUsageLimit;
    private SortOrder name;
}
