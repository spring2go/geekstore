/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.product;

import io.geekstore.types.common.ListOptions;
import io.geekstore.types.stock.StockMovementType;
import lombok.Data;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class StockMovementListOptions implements ListOptions {
    private Integer currentPage;
    private Integer pageSize;
    private StockMovementType type;
}
