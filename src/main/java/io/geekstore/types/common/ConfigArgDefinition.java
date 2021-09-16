/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.common;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class ConfigArgDefinition {
    private String name;
    private String type;
    private Boolean list;
    private String label;
    private String description;
    private Map<String, Object> ui = new HashMap<>();
}
