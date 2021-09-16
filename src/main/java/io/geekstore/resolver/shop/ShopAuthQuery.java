/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.shop;

import io.geekstore.common.RequestContext;
import io.geekstore.custom.security.Allow;
import io.geekstore.resolver.base.BaseAuthQuery;
import io.geekstore.service.AdministratorService;
import io.geekstore.service.UserService;
import io.geekstore.types.auth.CurrentUser;
import io.geekstore.types.common.Permission;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@Slf4j
public class ShopAuthQuery extends BaseAuthQuery implements GraphQLQueryResolver {

    public ShopAuthQuery(
            AdministratorService administratorService, UserService userService) {
        super(administratorService, userService);
    }

    /**
     * Returns information about the current authenticated User
     */
    @Allow(Permission.Authenticated)
    public CurrentUser me(DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        return super.me(ctx);
    }

}
