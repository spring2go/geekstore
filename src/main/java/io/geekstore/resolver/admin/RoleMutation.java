/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.RequestContext;
import io.geekstore.custom.security.Allow;
import io.geekstore.service.RoleService;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.Permission;
import io.geekstore.types.role.*;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class RoleMutation implements GraphQLMutationResolver {

    private final RoleService roleService;

    /**
     * Create a new Role
     */
    @Allow(Permission.CreateAdministrator)
    public Role createRole(CreateRoleInput input, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        return this.roleService.create(ctx, input);
    }

    /**
     * Update an existing Role
     */
    @Allow(Permission.UpdateAdministrator)
    public Role updateRole(UpdateRoleInput input, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        return this.roleService.update(ctx, input);
    }

    /**
     * Delete an existing Role
     */
    @Allow(Permission.DeleteAdministrator)
    public DeletionResponse deleteRole(Long id, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        return this.roleService.delete(ctx, id);
    }
}
