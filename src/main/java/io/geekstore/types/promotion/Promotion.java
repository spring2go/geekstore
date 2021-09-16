/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.promotion;

import io.geekstore.types.common.ConfigurableOperation;
import io.geekstore.types.common.Node;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class Promotion implements Node {
    private Long id;
    private Date createdAt;
    private Date updatedAt;
    private Date startsAt;
    private Date endsAt;
    private String couponCode;
    private Integer perCustomerUsageLimit;
    private String name;
    private Boolean enabled;
    private List<ConfigurableOperation> conditions = new ArrayList<>();
    private List<ConfigurableOperation> actions = new ArrayList<>();
}
