/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.RequestContext;
import io.geekstore.custom.security.Allow;
import io.geekstore.resolver.base.BaseAuthQuery;
import io.geekstore.service.AdministratorService;
import io.geekstore.service.UserService;
import io.geekstore.types.auth.CurrentUser;
import io.geekstore.types.common.Permission;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
public class AuthQuery extends BaseAuthQuery implements GraphQLQueryResolver {
    public AuthQuery(
            AdministratorService administratorService, UserService userService) {
        super(administratorService, userService);
    }

    @Allow({Permission.Authenticated, Permission.Owner})
    public CurrentUser adminMe(DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        return super.me(ctx);
    }
}
