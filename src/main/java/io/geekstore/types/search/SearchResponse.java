/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.search;

import io.geekstore.types.common.SearchInput;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class SearchResponse {
    private List<SearchResult> items = new ArrayList<>();
    private Integer totalItems;
    private List<FacetValueResult> facetValues = new ArrayList<>();
    private SearchInput searchInput; // 内部使用，GraphQL对外不可见
}
