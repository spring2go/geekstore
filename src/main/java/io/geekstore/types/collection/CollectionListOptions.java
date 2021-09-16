/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.collection;

import io.geekstore.types.common.ListOptions;
import lombok.Data;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class CollectionListOptions implements ListOptions {
    private Integer currentPage;
    private Integer pageSize;
    private CollectionSortParameter sort;
    private CollectionFilterParameter filter;
}
