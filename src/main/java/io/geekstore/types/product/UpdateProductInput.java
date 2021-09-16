/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.product;

import lombok.Data;

import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class UpdateProductInput {
    private Long id;
    private Boolean enabled;
    private Long featuredAssetId;
    private List<Long> assetIds;
    private List<Long> facetValueIds;
    private String name;
    private String slug;
    private String description;
}
