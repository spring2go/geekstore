/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.facet;

import io.geekstore.types.common.BooleanOperators;
import io.geekstore.types.common.DateOperators;
import io.geekstore.types.common.StringOperators;
import lombok.Data;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class FacetFilterParameter {
    private StringOperators name;
    private StringOperators code;
    private BooleanOperators privateOnly;
    private DateOperators createdAt;
    private DateOperators updatedAt;
}
