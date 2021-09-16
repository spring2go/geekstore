/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.auth;

import io.geekstore.common.RequestContext;
import io.geekstore.types.user.User;

import java.util.Map;

/**
 * An AuthenticationStrategy defines how a User (which can be a Customer in the Shop API or
 * an Administrator in the Admin API) may be authenticated.
 *
 * Real-world examples can be found in the [Authentication guide](/docs/developer-guide/authentication/).
 *
 * Created on Nov, 2020 by @author bobo
 */
public interface AuthenticationStrategy<Data> {
    /**
     * The name of the strategy, for example `google`, `facebook`, `keycloak`.
     */
    String getName();

    Data convertInputToData(Map<String, String> inputMap);
    /**
     * Used to authenticate a user with the authentication provider. This method
     * whill implement the provider-specific authentication logic, and should resolve to either a
     * {@link io.geekstore.types.user.User} object on success, or `null` on failure.
     */
    User authenticate(RequestContext ctx, Data data);

    /**
     * Called when a user logs out, and may perform any required tasks
     * related to the user logging out with the external provider.
     */
    void onLogOut(User user);
}
