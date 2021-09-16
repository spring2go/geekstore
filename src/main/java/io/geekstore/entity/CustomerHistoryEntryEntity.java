/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import io.geekstore.types.history.HistoryEntryType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on Nov, 2020 by @author bobo
 */
@TableName(value = "tb_customer_history_entry", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerHistoryEntryEntity extends BaseEntity {
    private Long administratorId;
    private HistoryEntryType type;
    private boolean privateOnly;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String,String> data = new HashMap<>();
    private Long customerId;
}
