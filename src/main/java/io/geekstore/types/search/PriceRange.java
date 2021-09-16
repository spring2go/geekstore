/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.search;

import lombok.Data;

/**
 * The price range where the result has more than one price
 *
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class PriceRange {
    private Integer min;
    private Integer max;
}
