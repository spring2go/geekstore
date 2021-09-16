/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.eventbus.events;

import io.geekstore.common.RequestContext;
import io.geekstore.entity.BaseEntity;
import io.geekstore.entity.ProductVariantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * This event is fired whenever a {@link io.geekstore.types.product.ProductVariant} is added, updated
 * or deleted.
 *
 * Created on Nov, 2020 by @author bobo
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductVariantEvent extends BaseEntity {
    private final RequestContext ctx;
    private final List<ProductVariantEntity> variants;
    private final String type;
}
