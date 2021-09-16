/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.auth;

import io.geekstore.common.Constant;
import io.geekstore.common.RequestContext;
import io.geekstore.entity.AuthenticationMethodEntity;
import io.geekstore.exception.UnauthorizedException;
import io.geekstore.exception.UserInputException;
import io.geekstore.mapper.AuthenticationMethodEntityMapper;
import io.geekstore.service.UserService;
import io.geekstore.types.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * This strategy implements a username/password credential-based authentication, with the credentials
 * being stored in the GeekStore database. This is the default method of authentication, and it is advised
 * to keep it configured unless there is a specific reason not to.
 *
 * Created on Nov, 2020 by @author bobo
 */
public class NativeAuthenticationStrategy implements AuthenticationStrategy<NativeAuthenticationData> {

    @Autowired
    private UserService userService;
    @Autowired
    private AuthenticationMethodEntityMapper authenticationMethodEntityMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";

    @Override
    public String getName() {
        return Constant.NATIVE_AUTH_STRATEGY_NAME;
    }

    @Override
    public NativeAuthenticationData convertInputToData(Map<String, String> inputMap) {
        NativeAuthenticationData data = new NativeAuthenticationData();
        data.setUsername(inputMap.get(KEY_USERNAME));
        if (StringUtils.isEmpty(data.getUsername())) {
            throw new UserInputException("username is empty");
        }
        data.setPassword(inputMap.get(KEY_PASSWORD));
        if (StringUtils.isEmpty(data.getPassword())) {
            throw new UserInputException("password is empty");
        }
        return data;
    }

    @Override
    public User authenticate(RequestContext ctx, NativeAuthenticationData data) {
        User user = this.getUserFromIdentifier(data.getUsername());
        boolean passwordMatch = this.verifyUserPassword(user.getId(), data.getPassword());
        if (!passwordMatch) {
            return null;
        }
        return user;
    }

    @Override
    public void onLogOut(User user) {
        // nothing to do
    }

    private User getUserFromIdentifier(String identifier) {
        User user = this.userService.findUserWithRoleByIdentifier(identifier);
        if (user == null) {
            throw new UnauthorizedException();
        }
        return user;
    }

    /**
     * Verify the provided password against the one we have for the given user.
     */
    public boolean verifyUserPassword(Long userId, String password) {
        AuthenticationMethodEntity nativeAuthMethod =
                this.userService.getNativeAuthMethodEntityByUserId(userId);

        boolean passwordMatches = this.passwordEncoder.matches(password, nativeAuthMethod.getPasswordHash());

        return passwordMatches;
    }
}
