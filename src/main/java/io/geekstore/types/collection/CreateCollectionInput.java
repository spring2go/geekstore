/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.collection;

import io.geekstore.types.common.ConfigurableOperationInput;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class CreateCollectionInput {
    private Boolean privateOnly;
    private Long featuredAssetId;
    private List<Long> assetIds = new ArrayList<>();
    private Long parentId;
    private List<ConfigurableOperationInput> filters = new ArrayList<>();
    private String name;
    private String slug;
    private String description;
}
