/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.promotion.conditions;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.config.promotion.PromotionCondition;
import io.geekstore.config.promotion.utils.FacetValueChecker;
import io.geekstore.entity.OrderEntity;
import io.geekstore.entity.OrderLineEntity;
import io.geekstore.types.common.ConfigArgDefinition;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on Dec, 2020 by @author bobo
 */
@SuppressWarnings("Duplicates")
public class HasFacetValuesCondition extends PromotionCondition {

    private final Map<String, ConfigArgDefinition> argSpec;

    @Autowired
    private FacetValueChecker facetValueChecker;

    public HasFacetValuesCondition() {
        super(
                "at_least_n_with_facets",
                "Buy at least { minimum } products with the given facets"
        );

        Map<String, ConfigArgDefinition> argDefMap = new HashMap<>();
        ConfigArgDefinition argDef = new ConfigArgDefinition();
        argDefMap = new HashMap<>();
        argDef = new ConfigArgDefinition();
        argDef.setType("int");
        argDefMap.put("minimum", argDef);

        argDef = new ConfigArgDefinition();
        argDef.setType("ID");
        argDef.setList(true);
        argDef.setUi(ImmutableMap.of("component", "facet-value-form-input"));
        argDefMap.put("facets", argDef);

        argSpec = argDefMap;
    }

    @Override
    public boolean check(OrderEntity orderEntity, ConfigArgValues argValues) {
        int matches = 0;
        for (OrderLineEntity line : orderEntity.getLines()) {
            if (facetValueChecker.hasFacetValues(line, argValues.getIdList("facets"))) {
                matches += line.getQuantity();
            }
        }
        return matches >= argValues.getInteger("minimum");
    }

    @Override
    public Map<String, ConfigArgDefinition> getArgSpec() {
        return argSpec;
    }
}
