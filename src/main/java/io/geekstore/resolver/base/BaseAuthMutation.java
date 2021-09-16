/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.base;

import io.geekstore.common.ApiType;
import io.geekstore.common.Constant;
import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.config.auth.NativeAuthenticationStrategy;
import io.geekstore.config.session_cache.CachedSession;
import io.geekstore.custom.graphql.CustomGraphQLServletContext;
import io.geekstore.custom.security.SessionTokenHelper;
import io.geekstore.entity.AdministratorEntity;
import io.geekstore.exception.InternalServerError;
import io.geekstore.exception.UnauthorizedException;
import io.geekstore.service.AdministratorService;
import io.geekstore.service.AuthService;
import io.geekstore.service.ConfigService;
import io.geekstore.service.UserService;
import io.geekstore.types.auth.AuthenticationInput;
import io.geekstore.types.auth.CurrentUser;
import io.geekstore.types.auth.LoginResult;
import com.google.common.collect.ImmutableMap;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Slf4j
public abstract class BaseAuthMutation {
    protected final AuthService authService;
    protected final UserService userService;
    protected final AdministratorService administratorService;
    protected final ConfigService configService;
    protected final boolean nativeAuthStrategyIsConfigured;

    protected BaseAuthMutation(
            AuthService authService,
            UserService userService,
            AdministratorService administratorService,
            ConfigService configService) {
        this.authService = authService;
        this.userService = userService;
        this.administratorService = administratorService;
        this.configService = configService;
        this.nativeAuthStrategyIsConfigured =
                this.configService.getAuthConfig().getShopAuthenticationStrategy()
                        .stream().anyMatch(strategy -> Constant.NATIVE_AUTH_STRATEGY_NAME.equals(strategy.getName()));
    }

    /**
     * Attempts a login given the username and password of a user. If successful, returns
     * the user data and returns the token either in a cookie or in the response body.
     */
    protected Boolean logout(DataFetchingEnvironment dfe) {

        CustomGraphQLServletContext customGraphQLServletContext = dfe.getContext();
        RequestContext ctx = customGraphQLServletContext.getRequestContext();
        HttpServletRequest request = customGraphQLServletContext.getHttpServletRequest();
        HttpServletResponse response = customGraphQLServletContext.getHttpServletResponse();

        String token = SessionTokenHelper.extractSessionToken(
                request,
                this.configService.getAuthOptions().getTokenMethod());
        if (StringUtils.isEmpty(token)) return false;

        this.authService.destroyAuthenticatedSession(ctx, token);

        SessionTokenHelper.setSessionToken(
                "",
                false,
                this.configService.getAuthOptions(),
                request,
                response
        );
        return true;
    }

    protected LoginResult login(String username, String password, Boolean rememberMe, DataFetchingEnvironment dfe) {
        AuthenticationInput input = new AuthenticationInput();
        input.setMethod(Constant.NATIVE_AUTH_STRATEGY_NAME);
        input.setData(ImmutableMap.of(
                NativeAuthenticationStrategy.KEY_USERNAME, username,
                NativeAuthenticationStrategy.KEY_PASSWORD, password));

        LoginResult loginResult = this.authenticateAndCreateSession(input, rememberMe, dfe);
        return loginResult;
    }

    /**
     * Creates an authenticated session and sets the session token.
     */
    protected LoginResult authenticateAndCreateSession(
            AuthenticationInput input,
            Boolean rememberMe,
            DataFetchingEnvironment dfe) {

        CustomGraphQLServletContext customGraphQLServletContext = dfe.getContext();
        RequestContext ctx = customGraphQLServletContext.getRequestContext();
        HttpServletRequest request = customGraphQLServletContext.getHttpServletRequest();
        HttpServletResponse response = customGraphQLServletContext.getHttpServletResponse();

        ApiType apiType = ctx.getApiType();
        CachedSession session = this.authService.authenticate(ctx, apiType, input.getMethod(), input.getData());
        if (apiType != null && ApiType.ADMIN.equals(apiType)) {
            AdministratorEntity administratorEntity =
                    this.administratorService.findOneEntityByUserId(session.getUser().getId());
            if (administratorEntity == null) {
                throw new UnauthorizedException();
            }
        }
        SessionTokenHelper.setSessionToken(session.getToken(),
                BooleanUtils.toBoolean(rememberMe),
                this.configService.getAuthOptions(),
                request,
                response);
        LoginResult loginResult = new LoginResult();
        CurrentUser currentUser = BeanMapper.map(session.getUser(), CurrentUser.class);
        loginResult.setUser(currentUser);
        return loginResult;
    }

    /**
     * Update the password of an existing User.
     */
    protected boolean updatePassword(RequestContext ctx, String currentPassword, String newPassword) {
        Long activeUserId = ctx.getActiveUserId();
        if (activeUserId == null) {
            throw new InternalServerError("No active user");
        }
        return this.userService.updatePassword(activeUserId, currentPassword, newPassword);
    }

    protected void requireNativeAuthStrategy() {
        if (!this.nativeAuthStrategyIsConfigured) {
            String authStrategyNames = this.configService.getAuthConfig().getShopAuthenticationStrategy()
                    .stream().map(strategy -> strategy.getName()).collect(Collectors.joining(", "));
            String errorMessage =
                    "This GraphQL operation requires that the NativeAuthenticationStrategy be configured " +
                            "for the Shop API.\n" +
                            "Currently the following AuthenticationStrategies are enabled: " + authStrategyNames;
            log.error(errorMessage);
            throw new InternalServerError("Server Configuration Error");
        }
    }

}
