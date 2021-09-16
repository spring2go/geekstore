/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.promotion.conditions;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.config.promotion.PromotionCondition;
import io.geekstore.entity.OrderEntity;
import io.geekstore.types.common.ConfigArgDefinition;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on Dec, 2020 by @author bobo
 */
@SuppressWarnings("Duplicates")
public class MinimumOrderAmountCondition extends PromotionCondition {
    private final Map<String, ConfigArgDefinition> argSpec;

    public MinimumOrderAmountCondition() {
        super(
                "minimum_order_amount",
                "If order total is greater than { amount }",
                10
        );
        Map<String, ConfigArgDefinition> argDefMap = new HashMap<>();
        ConfigArgDefinition argDef = new ConfigArgDefinition();
        argDef.setType("int");
        argDef.setUi(ImmutableMap.of("component", "currency-form-input"));
        argDefMap.put("amount", argDef);
        argSpec = argDefMap;
    }

    @Override
    public boolean check(OrderEntity orderEntity, ConfigArgValues argValues) {
        return orderEntity.getSubTotal() >= argValues.getInteger("amount");
    }

    @Override
    public Map<String, ConfigArgDefinition> getArgSpec() {
        return argSpec;
    }
}
