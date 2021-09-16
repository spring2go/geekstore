/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.eventbus.events;

import io.geekstore.common.RequestContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This event is fired when an attempt is made to log in via the shop or admin API `login` mutation.
 * The `strategy` represents the name of the AuthenticationStrategy used in the login attempt.
 * If the `native` strategy is used, the additional `identifier` property will be available.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AttemptedLoginEvent extends BaseEvent {
    private final RequestContext ctx;
    private final String strategy;
    private final String identifier;
}
