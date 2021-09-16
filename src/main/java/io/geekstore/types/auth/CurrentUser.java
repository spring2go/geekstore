/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.auth;

import io.geekstore.types.common.Permission;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class CurrentUser {
    private Long id;
    private String identifier;
    private List<Permission> permissions = new ArrayList<>();
}
