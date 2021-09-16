/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.promotion.actions;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.config.promotion.PromotionItemAction;
import io.geekstore.entity.OrderItemEntity;
import io.geekstore.entity.OrderLineEntity;
import io.geekstore.types.common.ConfigArgDefinition;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on Dec, 2020 by @author bobo
 */
public class ProductDiscountAction extends PromotionItemAction {

    private final  Map<String, ConfigArgDefinition> argSpec;

    public ProductDiscountAction() {
        super(
                "products_percentage_discount",
                "Discount specified products by { discount }%"
        );

        Map<String, ConfigArgDefinition> argDefMap = new HashMap<>();
        ConfigArgDefinition argDef = new ConfigArgDefinition();

        argDef.setType("int");
        argDef.setUi(ImmutableMap.of("component", "number-form-input", "suffix", "%"));
        argDefMap.put("discount", argDef);

        argDef = new ConfigArgDefinition();

        argDef.setType("ID");
        argDef.setList(true);
        argDef.setUi(ImmutableMap.of("component", "product-selector-form-input"));
        argDefMap.put("productVariantIds", argDef);

        argSpec = argDefMap;
    }

    @Override
    public float execute(OrderItemEntity orderItemEntity, OrderLineEntity orderLineEntity, ConfigArgValues argValues) {
        List<Long> ids = argValues.getIdList("productVariantIds");
        if (ids.contains(orderLineEntity.getProductVariantId())) {
            return orderItemEntity.getUnitPriceWithPromotions() *
                    (argValues.getInteger("discount") / 100.0F) * -1;
        }
        return 0.0F;
    }

    @Override
    public Map<String, ConfigArgDefinition> getArgSpec() {
        return argSpec;
    }
}
