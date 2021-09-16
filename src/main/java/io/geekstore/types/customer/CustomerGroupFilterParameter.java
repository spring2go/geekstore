/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.customer;

import io.geekstore.types.common.DateOperators;
import io.geekstore.types.common.StringOperators;
import lombok.Data;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class CustomerGroupFilterParameter {
    private StringOperators name;
    private DateOperators createdAt;
    private DateOperators updatedAt;
}
