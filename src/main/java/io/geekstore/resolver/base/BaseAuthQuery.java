/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.base;

import io.geekstore.common.ApiType;
import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.AdministratorEntity;
import io.geekstore.exception.ForbiddenException;
import io.geekstore.service.AdministratorService;
import io.geekstore.service.UserService;
import io.geekstore.types.auth.CurrentUser;
import io.geekstore.types.user.User;
import lombok.extern.slf4j.Slf4j;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Slf4j
public abstract class BaseAuthQuery {
    protected final AdministratorService administratorService;
    protected final UserService userService;

    protected BaseAuthQuery(
            AdministratorService administratorService, UserService userService) {
        this.administratorService = administratorService;
        this.userService = userService;
    }

    /**
     * Returns information about the current authenticated user.
     */
    protected CurrentUser me(RequestContext ctx) {
        Long userId = ctx.getActiveUserId();
        if (ApiType.ADMIN.equals(ctx.getApiType())) {
            AdministratorEntity administratorEntity = this.administratorService.findOneEntityByUserId(userId);
            if (administratorEntity == null) throw new ForbiddenException();
        }
        if (userId == null) return null;
        User user = this.userService.findUserWithRolesById(userId);
        return BeanMapper.map(user, CurrentUser.class);
    }

}
