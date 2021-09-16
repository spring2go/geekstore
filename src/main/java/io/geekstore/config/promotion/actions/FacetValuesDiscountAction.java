/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.promotion.actions;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.config.promotion.PromotionItemAction;
import io.geekstore.config.promotion.utils.FacetValueChecker;
import io.geekstore.entity.OrderItemEntity;
import io.geekstore.entity.OrderLineEntity;
import io.geekstore.types.common.ConfigArgDefinition;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on Dec, 2020 by @author bobo
 */
public class FacetValuesDiscountAction extends PromotionItemAction {
    @Autowired
    private FacetValueChecker facetValueChecker;

    private final  Map<String, ConfigArgDefinition> argSpec;

    public FacetValuesDiscountAction() {
        super(
                "facet_based_discount",
                "Discount products with these facets by { discount }%"
        );
        Map<String, ConfigArgDefinition> argDefMap = new HashMap<>();
        ConfigArgDefinition argDef = new ConfigArgDefinition();

        argDef.setType("int");
        argDef.setUi(ImmutableMap.of("component", "number-form-input", "suffix", "%"));
        argDefMap.put("discount", argDef);

        argDef = new ConfigArgDefinition();
        argDef.setType("ID");
        argDef.setList(true);
        argDef.setUi(ImmutableMap.of("component", "facet-value-form-input"));
        argDefMap.put("facets", argDef);

        argSpec = argDefMap;
    }

    @Override
    public float execute(OrderItemEntity orderItemEntity, OrderLineEntity orderLineEntity, ConfigArgValues argValues) {
        if (facetValueChecker.hasFacetValues(orderLineEntity, argValues.getIdList("facets"))) {
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
