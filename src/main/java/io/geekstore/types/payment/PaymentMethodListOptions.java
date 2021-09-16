/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.payment;

import io.geekstore.types.common.ListOptions;
import lombok.Data;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class PaymentMethodListOptions implements ListOptions {
    private Integer currentPage;
    private Integer pageSize;
    private PaymentMethodSortParameter sort;
    private PaymentMethodFilterParameter filter;
}
