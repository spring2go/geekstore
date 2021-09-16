/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A ProductOption is used to differentiate {@link ProductVariant}s from one another.
 *
 * Created on Nov, 2020 by @author bobo
 */
@TableName(value = "tb_product_option")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductOptionEntity extends BaseEntity {
    private String name;
    private String code;
    private Long groupId;
}
