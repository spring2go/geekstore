/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.shipping;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class TestShippingMethodQuote {
    private Integer price;
    private String description;
    private Map<String, String> metadata = new HashMap<>();
}
