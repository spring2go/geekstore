/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.search;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class SearchResult {
    private String sku;
    private String slug;
    private Boolean enabled;
    private Long productId;
    private String productName;
    private SearchResultAsset productAsset;
    private Long productVariantId;
    private String productVariantName;
    private SearchResultAsset productVariantAsset;
    private Integer price;
    private String description;
    private List<Long> facetIds = new ArrayList<>();
    private List<Long> facetValueIds = new ArrayList<>();
    /**
     * An array of ids of the Collections in which this result appears.
     */
    private List<Long> collectionIds = new ArrayList<>();
    /**
     * A relevance score for the result. Differs between database implementations
     */
    private Float score;
}
