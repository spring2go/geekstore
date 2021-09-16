/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * A ProductVariant represents a single stock keeping unit (SKU) in the sotre's inventory.
 * Whereas a {@link io.geekstore.types.product.Product} is a "container" of variants, the variant itself
 * holds the data on price, etc. When on adds items to their cart, they are adding ProductVariant, not Products.
 *
 * Created on Nov, 2020 by @author bobo
 */
@TableName(value = "tb_product_variant")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductVariantEntity extends BaseEntity {
    private Date deletedAt;
    private String name;
    private boolean enabled = true;
    private String sku;
    private Integer price;
    private Long featuredAssetId;
    private Long productId;
    private Integer stockOnHand = 0;
    private boolean trackInventory;
}
