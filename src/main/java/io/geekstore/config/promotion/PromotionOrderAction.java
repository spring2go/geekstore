/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.promotion;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.entity.OrderEntity;

/**
 * Represents a PromotionAction which applies to the {@link OrderEntity} as a whole.
 *
 * Created on Dec, 2020 by @author bobo
 */
public abstract class PromotionOrderAction extends PromotionAction {
    protected PromotionOrderAction(
            String code,
            String description) {
        super(code, description, null);
    }

    protected PromotionOrderAction(
            String code,
            String description,
            Integer priorityValue) {
        super(code, description, priorityValue);
    }

    public abstract float execute(OrderEntity order, ConfigArgValues argValues);
}
