/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * This entity represents a customer of the store, typically an individual person. A Customer can be
 * a guest, in which case it has no associated {@link io.geekstore.types.user.User}. Customers with
 * registered account will have an associated User entity.
 *
 * Created on Nov, 2020 by @author bobo
 */
@TableName(value = "tb_customer")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerEntity extends BaseEntity {
    private Date deletedAt;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String emailAddress;
    private Long userId;
}
