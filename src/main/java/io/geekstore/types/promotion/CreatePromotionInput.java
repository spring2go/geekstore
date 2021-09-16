/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.promotion;

import io.geekstore.types.common.ConfigurableOperationInput;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Data
public class CreatePromotionInput {
    private String name;
    private Boolean enabled;
    private Date startsAt;
    private Date endsAt;
    private String couponCode;
    private Integer perCustomerUsageLimit;
    private List<ConfigurableOperationInput> conditions = new ArrayList<>();
    private List<ConfigurableOperationInput> actions = new ArrayList<>();
}
