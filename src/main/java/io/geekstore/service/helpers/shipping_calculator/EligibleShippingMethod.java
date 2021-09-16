/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers.shipping_calculator;

import io.geekstore.config.shipping_method.ShippingCalculationResult;
import io.geekstore.entity.ShippingMethodEntity;
import lombok.Data;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class EligibleShippingMethod {
    private ShippingMethodEntity method;
    private ShippingCalculationResult result;
}
