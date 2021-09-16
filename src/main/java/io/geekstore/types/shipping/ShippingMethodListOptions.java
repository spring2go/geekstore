/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.shipping;

import io.geekstore.types.common.ListOptions;
import lombok.Data;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class ShippingMethodListOptions implements ListOptions {
    private Integer currentPage;
    private Integer pageSize;
    private ShippingMethodSortParameter sort;
    private ShippingMethodFilterParameter filter;
}
