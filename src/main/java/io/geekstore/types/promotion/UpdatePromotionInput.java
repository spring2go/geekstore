/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.promotion;

import io.geekstore.types.common.ConfigurableOperationInput;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class UpdatePromotionInput {
    private Long id;
    private String name;
    private Boolean enabled;
    private Date startsAt;
    private Date endsAt;
    private String couponCode;
    private Integer perCustomerUsageLimit;
    private List<ConfigurableOperationInput> conditions;
    private List<ConfigurableOperationInput> actions;
}
