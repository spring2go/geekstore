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
 * An administrator user who has access to the admin ui.
 *
 * Created on Nov, 2020 by @author bobo
 */
@TableName(value = "tb_administrator")
@Data
@EqualsAndHashCode(callSuper = true)
public class AdministratorEntity extends BaseEntity {
    private Date deletedAt;
    private String firstName = "";
    private String lastName = "";
    private String emailAddress = "";
    private Long userId;
}
