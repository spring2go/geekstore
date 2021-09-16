/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.options;

import io.geekstore.common.Constant;
import lombok.Data;

/**
 * The AuthOptions define how authentication is managed.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class AuthOptions {
    /**
     * Disable authentication & permissions checks.
     * NEVER set the to true in production. It exists
     * only to aid certain development tasks.
     *
     * @default false
     */
    private boolean disableAuth = false;
    /**
     * Sets the method by which the session token is delivered and read.
     *
     * * 'cookie': Upon login, a 'Set-Cookie' header will be returned to the client, setting a
     *   cookie containing the session token. A browser-based client (making requests with credentials)
     *   should automatically send the session cookie with each request.
     * * 'bearer': Upon login, the token is returned in the response and should be then stored by the
     *   client app. Each request should include the header 'Authorization: Bearer <token>'.
     *
     * Node that if the bearer method is used, GeekStore will automatically expose the configured
     * `authTokenHeaderKey` in the server's CORS configuration (adding `Access-Control-Expose-Headers:
     * geekstore-shared-token` by default).
     *
     * @default 'cookie'
     */
    private TokenMethod tokenMethod = TokenMethod.cookie;

    /**
     * Options related to the handling of cookies when using the 'cookie' tokenMethod.
     */
    private CookieOptions cookieOptions = new CookieOptions();

    /**
     * Sets the header property which will be used to send the shared token when using the 'bearer' method.
     *
     * @default 'geekstore-shared-token'
     */
    private String authTokenHeaderKey = Constant.DEFAULT_AUTH_TOKEN_HEADER_KEY;
    /**
     * Session duration, i.e. the time which must elapse from the last authenticated request
     * after which the user must re-authenticate.
     *
     * Expressed as a string describing a time span per
     * [zeit/ms](https://github.com/zeit/ms.js).  Eg: `60`, `'2 days'`, `'10h'`, `'7d'`
     *
     * @default '7d'
     */
    private String sessionDuration = "7d";


    /**
     * The "time to live" of a given item in the session cache. This determines the length of time (in seconds)
     * that a cache entry is kept before considered "stale" and being replaced with fresh data taken from the database.
     *
     * @default 300
     */
    private int sessionCacheTTL = 300;

    /**
     * Determines whether new User accounts require verification of their email address.
     *
     * If set to "true", when registering via the `registerCustomerAccount` mutation, one should *not* set the
     * `password` property - doing so will result in an error. Instead, the password is set at a later stage
     * (once the email with the verification token has been opened) via the `verifyCustomerAccount` mutation.
     *
     * @defaut true
     */
    private boolean requireVerification = true;
    /**
     * Sets the length of time that a verification token is valid for, after which the verification token
     * must be refreshed.
     *
     * Expressed as a string describing a time span per
     * [zeit/ms](https://github.com/zeit/ms.js).  Eg: `60`, `'2 days'`, `'10h'`, `'7d'`
     *
     * @default '7d'
     */
    private String verificationTokenDuration = "7d";

    /**
     * Configures the credentials to be used to create a superadmin
     */
    private SuperadminCredentials superadminCredentials;
}
