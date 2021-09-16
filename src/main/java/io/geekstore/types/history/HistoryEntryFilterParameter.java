/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.history;

import io.geekstore.types.common.BooleanOperators;
import io.geekstore.types.common.DateOperators;
import io.geekstore.types.common.StringOperators;
import lombok.Data;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class HistoryEntryFilterParameter {
    private DateOperators createdAt;
    private DateOperators updatedAt;
    private BooleanOperators isPublic;
    private StringOperators type;
}
