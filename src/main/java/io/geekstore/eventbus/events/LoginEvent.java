/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.eventbus.events;

import io.geekstore.common.RequestContext;
import io.geekstore.types.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This event is fired when a user successfully logs in via the shop or admin API `login` mutation.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginEvent extends BaseEvent {
    private final RequestContext ctx;
    private final User uer;
}
