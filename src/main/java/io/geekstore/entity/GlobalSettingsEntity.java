/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

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
@TableName(value = "tb_global_settings", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlobalSettingsEntity extends BaseEntity {
    /**
     * Specifies the default value for inventory tracking for ProductVariants.
     * Can be overridden per ProductVariant, but this value determines the default if not otherwise specified.
     */
    private boolean trackInventory = false;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> customFields = new HashMap<>();
}
