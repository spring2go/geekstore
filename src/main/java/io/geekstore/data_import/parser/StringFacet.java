/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.data_import.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StringFacet {
    private String facet;
    private String value;
}
