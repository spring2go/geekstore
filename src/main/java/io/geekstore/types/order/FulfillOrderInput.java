/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.order;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class FulfillOrderInput {
    private List<OrderLineInput> lines = new ArrayList<>();
    private String method;
    private String trackingCode;
}
