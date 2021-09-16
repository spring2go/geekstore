/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A grouping of one or more {@link io.geekstore.types.product.ProductOption}
 *
 * Created on Nov, 2020 by @author bobo
 */
@TableName(value = "tb_product_option_group")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductOptionGroupEntity extends BaseEntity {
    private String name;
    private String code;
//    private Long productId;
}
