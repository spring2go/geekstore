/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.shipping;

import io.geekstore.types.common.ConfigurableOperationInput;
import lombok.Data;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class CreateShippingMethodInput {
    private String code;
    private String description;
    private ConfigurableOperationInput checker;
    private ConfigurableOperationInput calculator;
}
