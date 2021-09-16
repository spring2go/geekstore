/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.eventbus.events;

import io.geekstore.common.RequestContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This event is fired when a user logs out via the shop or admin API `logout` mutation.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LogoutEvent extends BaseEvent {
    private final RequestContext ctx;
}
