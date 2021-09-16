/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.stock;

import io.geekstore.types.common.PaginatedList;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class StockMovementList implements PaginatedList<StockMovement> {
    private List<StockMovement> items = new ArrayList<>();
    private Integer totalItems;
}
