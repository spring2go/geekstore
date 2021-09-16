/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.common;

import lombok.Data;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class Adjustment {
    private String adjustmentSource;
    private AdjustmentType type;
    private String description;
    private Integer amount;
}
