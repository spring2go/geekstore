/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.data_import;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * An object defining initial settings for a new GeekStore installation
 *
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class InitialData {
    private List<ShippingMethodData> shippingMethods = new ArrayList<>();
    private List<CollectionDefinition> colletions = new ArrayList<>();
}
