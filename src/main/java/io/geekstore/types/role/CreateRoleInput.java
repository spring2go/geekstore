/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.role;

import io.geekstore.types.common.Permission;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class CreateRoleInput {
    private String code;
    private String description;
    private List<Permission> permissions = new ArrayList<>();
}
