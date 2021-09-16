/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.payment;

import io.geekstore.types.common.Node;
import lombok.Data;

import java.util.*;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class Payment implements Node {
    private Long id;
    private Date createdAt;
    private Date updatedAt;
    private String method;
    private Integer amount;
    private String state;
    private String transactionId;
    private String errorMessage;
    private List<Refund> refunds = new ArrayList<>();
    private Map<String, String> metadata = new HashMap<>();
}
