/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.common;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class SearchInput {
    private String term;
    private List<Long> facetValueIds = new ArrayList<>();
    private LogicalOperator facetValueOperator;
    private Long collectionId;
    private String collectionSlug;
    private Integer currentPage;
    private Integer pageSize;
    private SearchResultSortParameter sort;
}
