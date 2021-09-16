/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.AdministratorEntity;
import io.geekstore.service.AdministratorService;
import io.geekstore.types.administrator.Administrator;
import io.geekstore.types.administrator.CreateAdministratorInput;
import io.geekstore.types.administrator.UpdateAdministratorInput;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.Permission;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class AdministratorMutation implements GraphQLMutationResolver {

    private final AdministratorService administratorService;

    @Allow(Permission.CreateAdministrator)
    public Administrator createAdministrator(CreateAdministratorInput input, DataFetchingEnvironment dfe) {
        AdministratorEntity administratorEntity = this.administratorService.create(input);
        if (administratorEntity == null) return null;
        return BeanMapper.map(administratorEntity, Administrator.class);
    }

    @Allow(Permission.UpdateAdministrator)
    public Administrator updateAdministrator(UpdateAdministratorInput input, DataFetchingEnvironment dfe) {
        AdministratorEntity administratorEntity = this.administratorService.update(input);
        if (administratorEntity == null) return null;
        return BeanMapper.map(administratorEntity, Administrator.class);
    }

    @Allow(Permission.DeleteAdministrator)
    public DeletionResponse deleteAdministrator(Long id, DataFetchingEnvironment dfe) {
        return this.administratorService.softDelete(id);
    }

    @Allow(Permission.UpdateAdministrator)
    public Administrator assignRoleToAdministrator(Long administratorId, Long roleId, DataFetchingEnvironment dfe) {
        AdministratorEntity administratorEntity = this.administratorService.assignRole(administratorId, roleId);
        if (administratorEntity == null) return null;
        return BeanMapper.map(administratorEntity, Administrator.class);
    }
}
