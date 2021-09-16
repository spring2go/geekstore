/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.product;

import io.geekstore.types.asset.Asset;
import io.geekstore.types.common.Node;
import io.geekstore.types.facet.FacetValue;
import io.geekstore.types.stock.StockMovementList;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class ProductVariant implements Node {
    private Long id;
    private Product product;
    private Long productId;
    private Date createdAt;
    private Date updatedAt;
    private String sku;
    private String name;
    // 该字段对GraphQL不可见，仅内部使用
    private Long featuredAssetId;
    private Asset featuredAsset;
    private List<Asset> assets = new ArrayList<>();
    private Integer price;
    private List<ProductOption> options = new ArrayList<>();
    private List<FacetValue> facetValues = new ArrayList<>();
    private boolean enabled; // admin only
    private Integer stockOnHand; // admin only
    private boolean trackInventory; // admin only
    private StockMovementList stockMovements; // admin only
}
