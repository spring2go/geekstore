/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This entity represents a fulfillment of an Order or part of it, i.e.
 * the {@link io.geekstore.types.order.OrderItem}s have been delivered to the Customer after successful payment.
 *
 * Created on Dec, 2020 by @author bobo
 */
@TableName(value = "tb_fulfillment")
@Data
@EqualsAndHashCode(callSuper = true)
public class FulfillmentEntity extends BaseEntity {
    private String trackingCode = "";
    private String method;
}
