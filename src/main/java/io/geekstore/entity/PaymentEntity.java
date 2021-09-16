/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import io.geekstore.service.helpers.payment_state_machine.PaymentState;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * A Payment represents a single payment transaction and exists in a well-defined state defined by the
 * {@link PaymentState} type.
 *
 * Created on Dec, 2020 by @author bobo
 */
@TableName(value = "tb_payment", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentEntity extends BaseEntity {
    private String method;
    private Integer amount;
    private PaymentState state;
    private String errorMessage;
    private String transactionId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> metadata = new HashMap<>();
    private Long orderId;
}
