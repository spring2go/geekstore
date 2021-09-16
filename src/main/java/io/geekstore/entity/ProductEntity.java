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
 * A Product contains one or more {@link io.geekstore.types.product.ProductVariant}s and serves as a
 * container for those variants, providing an overall name, description etc.
 *
 * Created on Nov, 2020 by @author bobo
 */
@TableName(value = "tb_product")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductEntity extends BaseEntity {
    private Date deletedAt;
    private String name;
    private String slug;
    private String description;
    private boolean enabled = true;
    private Long featuredAssetId;
}
