/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.product;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class CreateProductInput {
    private Long featuredAssetId;
    private List<Long> assetIds = new ArrayList<>();
    private List<Long> facetValueIds = new ArrayList<>();
    private String name;
    private String slug;
    private String description;
}
