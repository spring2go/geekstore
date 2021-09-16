/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.customer;

import io.geekstore.types.common.SortOrder;
import lombok.Data;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class CustomerGroupSortParameter {
    private SortOrder id;
    private SortOrder createdAt;
    private SortOrder updatedAt;
    private SortOrder name;
}
