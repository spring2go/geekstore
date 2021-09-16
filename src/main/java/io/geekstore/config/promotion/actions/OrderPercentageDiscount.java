/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.promotion.actions;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.config.promotion.PromotionOrderAction;
import io.geekstore.entity.OrderEntity;
import io.geekstore.types.common.ConfigArgDefinition;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on Dec, 2020 by @author bobo
 */
@SuppressWarnings("Duplicates")
public class OrderPercentageDiscount extends PromotionOrderAction {

    private final  Map<String, ConfigArgDefinition> argSpec;

    public OrderPercentageDiscount() {
        super(
                "order_percentage_discount",
                "Discount order by { discount }%"
        );
        Map<String, ConfigArgDefinition> argDefMap = new HashMap<>();
        ConfigArgDefinition argDef = new ConfigArgDefinition();

        argDef.setType("int");
        argDef.setUi(ImmutableMap.of("component", "number-form-input", "suffix", "%"));
        argDefMap.put("discount", argDef);

        argSpec = argDefMap;
    }

    @Override
    public float execute(OrderEntity orderEntity, ConfigArgValues argValues) {
        return orderEntity.getSubTotal() * (argValues.getInteger("discount") / 100.0F) * -1;
    }

    @Override
    public Map<String, ConfigArgDefinition> getArgSpec() {
        return argSpec;
    }
}
