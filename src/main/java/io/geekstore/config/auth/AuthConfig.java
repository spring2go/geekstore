/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.auth;

import io.geekstore.config.session_cache.SessionCacheStrategy;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * The AuthConfig define how shared & shared is managed.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class AuthConfig {

    /**
     * Configures one or more AuthenticationStrategies which defines how authentication
     * is handled in the Shop API.
     *
     * @default NativeAuthenticationStrategy
     */
    private final List<AuthenticationStrategy> shopAuthenticationStrategy;
    /**
     * Configures one or more AuthenticationStrategies which defines how authentication
     * is handled in the Admin API.
     */
    private final List<AuthenticationStrategy> adminAuthenticationStrategy;
    /**
     * This strategy defines how sessions will be cached. By default, sessions are cached using a simple
     * in-memory caching strategy which is suitable for development and low-traffic, single-instance
     * deployments.
     */
    @Autowired
    private SessionCacheStrategy sessionCacheStrategy;
}
