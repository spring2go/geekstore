/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.order;

import io.geekstore.types.asset.Asset;
import io.geekstore.types.common.Adjustment;
import io.geekstore.types.common.Node;
import io.geekstore.types.product.ProductVariant;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class OrderLine implements Node {
    private Long id;
    private Date createdAt;
    private Date updatedAt;
    private ProductVariant productVariant;
    private Long productVariantId; // 内部使用，GraphQL不可见
    private Asset featuredAsset;
    private Long featuredAssetId; // 内部使用，GraphQL不可见
    private Integer unitPrice;
    private Integer quantity;
    private List<OrderItem> items = new ArrayList<>();
    private Integer totalPrice;
    private List<Adjustment> adjustments = new ArrayList<>();
    private Order order;
    private Long orderId; // 内部使用，GraphQL不可见
}
