/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.custom.security.Allow;
import io.geekstore.service.RoleService;
import io.geekstore.types.common.Permission;
import io.geekstore.types.role.Role;
import io.geekstore.types.role.RoleList;
import io.geekstore.types.role.RoleListOptions;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class RoleQuery implements GraphQLQueryResolver {

    private final RoleService roleService;

    /**
     * Query
     */
    @Allow(Permission.ReadAdministrator)
    public RoleList roles(RoleListOptions options, DataFetchingEnvironment dfe) {
        return roleService.findAll(options);
    }

    @Allow(Permission.ReadAdministrator)
    public Role role(Long id, DataFetchingEnvironment dfe) {
        return roleService.findOne(id);
    }
}
