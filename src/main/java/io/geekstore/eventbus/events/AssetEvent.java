/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.eventbus.events;


import io.geekstore.common.RequestContext;
import io.geekstore.entity.AssetEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This event is fired whenere an {@link io.geekstore.types.asset.Asset} is added, updated
 * or deleted.
 *
 * Created on Nov, 2020 by @author bobo
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AssetEvent extends BaseEvent {
    private final RequestContext ctx;
    private final AssetEntity asset;
    private final String type; // 'created' | 'updated' | 'deleted'
}
