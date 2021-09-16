/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.eventbus.events;

import io.geekstore.common.RequestContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ApplyCollectionFilterEvent extends BaseEvent {
    private final RequestContext ctx;
    private final List<Long> collectionIds;
}
