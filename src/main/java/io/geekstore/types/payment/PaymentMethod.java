/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.payment;

import io.geekstore.types.common.ConfigArg;
import io.geekstore.types.common.ConfigurableOperationDefinition;
import io.geekstore.types.common.Node;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class PaymentMethod implements Node {
    private Long id;
    private Date createdAt;
    private Date updatedAt;
    private String code;
    private Boolean enabled;
    private List<ConfigArg> configArgs = new ArrayList<>();
    private ConfigurableOperationDefinition definition;
}
