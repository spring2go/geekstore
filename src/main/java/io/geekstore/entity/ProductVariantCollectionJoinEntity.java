/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created on Nov, 2020 by @author bobo
 */
@TableName(value = "tb_product_variant_collection_join")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductVariantCollectionJoinEntity extends BaseEntity {
    private Long productVariantId;
    private Long collectionId;
}
