/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.product;

import io.geekstore.types.common.BooleanOperators;
import io.geekstore.types.common.DateOperators;
import io.geekstore.types.common.StringOperators;
import lombok.Data;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class ProductFilterParameter {
    private BooleanOperators enabled;
    private DateOperators createdAt;
    private DateOperators updatedAt;
    private StringOperators name;
    private StringOperators slug;
    private StringOperators description;
}
