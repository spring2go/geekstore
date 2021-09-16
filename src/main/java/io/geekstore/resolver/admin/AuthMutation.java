/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.custom.security.Allow;
import io.geekstore.resolver.base.BaseAuthMutation;
import io.geekstore.service.AdministratorService;
import io.geekstore.service.AuthService;
import io.geekstore.service.ConfigService;
import io.geekstore.service.UserService;
import io.geekstore.types.auth.AuthenticationInput;
import io.geekstore.types.auth.LoginResult;
import io.geekstore.types.common.Permission;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
public class AuthMutation extends BaseAuthMutation implements GraphQLMutationResolver {
    public AuthMutation(AuthService authService,
                        UserService userService,
                        AdministratorService administratorService,
                        ConfigService configService) {
        super(authService, userService, administratorService, configService);
    }

    /**
     * Authenticates the user using the native authentication strategy. This mutation is a alias for
     * `authenticate({ native: { ... }})`
     */
    @Allow(Permission.Public)
    public LoginResult adminLogin(String username, String password, Boolean rememberMe, DataFetchingEnvironment dfe) {
        return super.login(username, password, rememberMe, dfe);
    }

    /**
     * Authenticates the user using a named authentication strategy
     */
    @Allow(Permission.Public)
    public LoginResult adminAuthenticate(AuthenticationInput input, Boolean rememberMe, DataFetchingEnvironment dfe) {
        return super.authenticateAndCreateSession(input, rememberMe, dfe);
    }

    @Allow(Permission.Public)
    public Boolean adminLogout(DataFetchingEnvironment dfe) {
        return super.logout(dfe);
    }
}
