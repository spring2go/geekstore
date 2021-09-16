/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.shipping;

import lombok.Data;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class TestShippingMethodResult {
    private Boolean eligible;
    private TestShippingMethodQuote quote;
}
