/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import io.geekstore.types.common.Permission;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * A Role represents a collection of permissions which determine the authorization
 * level of a {@link io.geekstore.types.user.User}.
 *
 * Created on Nov, 2020 by @author bobo
 */
@TableName(value = "tb_role", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleEntity extends BaseEntity {
    private String code = "";
    private String description = "";
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Permission> permissions = new ArrayList<>();
}
