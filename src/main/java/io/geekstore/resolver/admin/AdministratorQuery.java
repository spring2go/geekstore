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
import io.geekstore.types.administrator.AdministratorList;
import io.geekstore.types.administrator.AdministratorListOptions;
import io.geekstore.types.common.Permission;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class AdministratorQuery implements GraphQLQueryResolver {

    private final AdministratorService administratorService;

    @Allow(Permission.ReadAdministrator)
    public AdministratorList administrators(AdministratorListOptions options, DataFetchingEnvironment dfe) {
        return administratorService.findAll(options);
    }

    @Allow(Permission.ReadAdministrator)
    public Administrator administrator(Long id, DataFetchingEnvironment dfe) {
        AdministratorEntity administratorEntity = administratorService.findOneEntity(id);
        if (administratorEntity == null) return null;
        return BeanMapper.map(administratorEntity, Administrator.class);
    }
}
