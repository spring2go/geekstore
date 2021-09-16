/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.facet;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class CreateFacetInput {
    private String code;
    private String name;
    private Boolean privateOnly;
    private List<CreateFacetValueWithFacetInput> values = new ArrayList<>();
}
