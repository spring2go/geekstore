/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.eventbus.events;

import io.geekstore.common.RequestContext;
import io.geekstore.entity.CollectionEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
 * This event is fired whenever a Collection is modified in some way. The `productVariantIds`
 * argument is an array of ids of all ProductVariants which:
 *
 * 1. were part of this collection prior to modification and are no longer
 * 2. are now part of this collection after modification but were not before
 *
 * Created on Nov, 2020 by @author bobo
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CollectionModificationEvent extends BaseEvent {
    private final RequestContext ctx;
    private final CollectionEntity collection;
    private final Set<Long> productVariantIds;
}
