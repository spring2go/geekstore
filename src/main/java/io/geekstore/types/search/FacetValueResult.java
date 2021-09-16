/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.search;

import io.geekstore.types.facet.FacetValue;
import lombok.Data;

/**
 * Which FacetValue are present in the products returned
 * by the search, and in what quantity.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class FacetValueResult {
    private FacetValue facetValue;
    private Integer count;
}
