/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.common;

import io.geekstore.config.promotion.PromotionAction;
import io.geekstore.config.promotion.PromotionCondition;
import io.geekstore.config.shipping_method.ShippingCalculator;
import io.geekstore.config.shipping_method.ShippingEligibilityChecker;
import io.geekstore.types.common.ConfigArgDefinition;
import io.geekstore.types.common.ConfigurableOperationDefinition;
import org.apache.commons.lang3.BooleanUtils;

import java.util.Map;

/**
 * A ConfigurableOperationDef is a special type of object used extensively by GeekStore to define
 * code blocks which have arguments which are configurable at run-time by the administrator.
 *
 * This is the mechanism use by:
 *
 * * {@link io.geekstore.config.collection.CollectionFilter}
 * * {@link PaymentMethodHandler}
 * * {@link PromotionAction}
 * * {@link PromotionCondition}
 * * {@link ShippingCalculator}
 * * {@link ShippingEligibilityChecker}
 *
 *
 * Created on Nov, 2020 by @author bobo
 */
public abstract class ConfigurableOperationDef {
    public abstract String getCode();
    public abstract Map<String, ConfigArgDefinition> getArgSpec();
    public abstract String getDescription();


    /**
     * Convert a ConfigurableOperationDef into a ConfigurableOperationDefinition object, typically
     * so that it can be sent via the API.
     */
    public ConfigurableOperationDefinition toGraphQLType() {
        ConfigurableOperationDefinition configurableOperationDefinition = new ConfigurableOperationDefinition();
        configurableOperationDefinition.setCode(getCode());
        configurableOperationDefinition.setDescription(getDescription());
        getArgSpec().forEach((name, arg) -> {
            ConfigArgDefinition configArgDefinition = new ConfigArgDefinition();
            configArgDefinition.setName(name);
            configArgDefinition.setType(arg.getType());
            configArgDefinition.setList(BooleanUtils.toBoolean(arg.getList()));
            configArgDefinition.setUi(arg.getUi());
            configArgDefinition.setLabel(arg.getLabel());
            configArgDefinition.setDescription(arg.getDescription());
            configurableOperationDefinition.getArgs().add(configArgDefinition);
        });
        return configurableOperationDefinition;
    }
}
