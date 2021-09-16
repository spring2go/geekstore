/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.promotion;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.entity.OrderItemEntity;
import io.geekstore.entity.OrderLineEntity;

/**
 * Represents a PromotionAction which applies to individual {@link OrderItemEntity}
 *
 * Created on Dec, 2020 by @author bobo
 */
public abstract class PromotionItemAction extends PromotionAction {

    protected PromotionItemAction(
            String code,
            String description) {
        super(code, description, null);
    }
    protected PromotionItemAction(String code,
                               String description,
                               Integer priorityValue) {
        super(code, description, priorityValue);
    }

    public abstract float execute(OrderItemEntity orderItem,
                         OrderLineEntity orderLine,
                         ConfigArgValues argValues);
}
