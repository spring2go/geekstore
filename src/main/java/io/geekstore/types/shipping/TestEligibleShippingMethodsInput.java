/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.shipping;

import io.geekstore.types.common.CreateAddressInput;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class TestEligibleShippingMethodsInput {
    private CreateAddressInput shippingAddress;
    private List<TestShippingMethodOrderLineInput> lines = new ArrayList<>();
}
