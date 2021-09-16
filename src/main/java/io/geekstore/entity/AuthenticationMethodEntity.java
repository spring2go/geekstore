/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
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
@TableName(value = "tb_auth_method")
@Data
@EqualsAndHashCode(callSuper = true)
public class AuthenticationMethodEntity extends BaseEntity {
    private Long userId;

    private boolean external; // external or native

    // begin for native shared
    private String identifier;
    private String passwordHash;
    /**
     * 参考：https://blog.csdn.net/qq_39403545/article/details/85334250
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String verificationToken;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String passwordResetToken;
    /**
     * A token issued when a User requests to change their identifer (typically an email address)
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String identifierChangeToken;
    /**
     * When a request has been made to change the User's identifier, the new identifier
     * will be stored here until it has been verified, after which it will replace
     * the current value of the `identifier` field.
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String pendingIdentifier;
    // end for native shared

    // begin for external shared
    private String strategy;
    private String externalIdentifier;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> metadata = new HashMap<>();
    // end for external shared
}
