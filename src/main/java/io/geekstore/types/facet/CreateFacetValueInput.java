/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.facet;

import lombok.Data;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class CreateFacetValueInput {
    private Long facetId;
    private String code;
    private String name;
}
