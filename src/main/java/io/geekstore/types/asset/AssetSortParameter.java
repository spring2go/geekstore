/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.asset;

import io.geekstore.types.common.SortOrder;
import lombok.Data;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class AssetSortParameter {
    private SortOrder id;
    private SortOrder name;
    private SortOrder createdAt;
    private SortOrder updatedAt;
}
