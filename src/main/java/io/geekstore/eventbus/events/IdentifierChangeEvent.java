/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.eventbus.events;

import io.geekstore.common.RequestContext;
import io.geekstore.entity.UserEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This event is fired when a registered user successfully changes the identifier (ie email address)
 * associated with their account.
 *
 * Created on Nov, 2020 by @author bobo
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class IdentifierChangeEvent extends BaseEvent {
    private final RequestContext ctx;
    private final UserEntity userEntity;
    private final String oldIdentifier;
}
