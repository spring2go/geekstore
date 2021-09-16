/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import io.geekstore.service.helpers.refund_state_machine.RefundState;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on Dec, 2020 by @author bobo
 */
@TableName(value = "tb_refund", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class RefundEntity extends BaseEntity {
    private Integer items;
    private Integer shipping;
    private Integer adjustment;
    private Integer total;
    private String method;
    private String reason;
    private RefundState state;
    private String transactionId;
    private Long paymentId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> metadata = new HashMap<>();
}
