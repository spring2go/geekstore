/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.eventbus.events;

import io.geekstore.common.RequestContext;
import io.geekstore.entity.ProductEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This event is fired whenever a {@link io.geekstore.types.product.Product} is added, updated
 * or deleted.
 *
 * Created on Nov, 2020 by @author bobo
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductEvent extends BaseEvent {
    private final RequestContext ctx;
    private final ProductEntity product;
    private final String type; // 'created' | 'updated' | 'deleted'
}
