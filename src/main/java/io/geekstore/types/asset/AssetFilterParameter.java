/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.asset;

import io.geekstore.types.common.DateOperators;
import io.geekstore.types.common.StringOperators;
import lombok.Data;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class AssetFilterParameter {
    private StringOperators name;
    private StringOperators type;
    private DateOperators createdAt;
    private DateOperators updatedAt;
}
