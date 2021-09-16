/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.role;

import io.geekstore.types.common.DateOperators;
import io.geekstore.types.common.StringOperators;
import lombok.Data;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class RoleFilterParameter {
    private StringOperators code;
    private StringOperators description;
    private DateOperators createdAt;
    private DateOperators updatedAt;
}
