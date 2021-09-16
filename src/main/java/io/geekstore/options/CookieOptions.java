/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.options;

import lombok.Data;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Options for the handling of the cookies used to track sessions (only applicable if
 * `authOptions.tokeMethod` is set to `'cookie'`).
 *
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class CookieOptions {
    /**
     * The name of the cookie to set.
     *
     * @default 'session'
     */
    private String name = "session";

    /**
     * A string which will be used as single key if keys is not provided.
     *
     * @default (random character string)
     */
    private String secret= RandomStringUtils.randomAlphabetic(12);

    /**
     * a string indicating the path of the cookie.
     */
    private String path = "/";

    /**
     * a string indicating the domain of the cookie (no default)
     */
    private String domain;

    /**
     * a enum indicating whether the cookie is a "same site" cookie (_false by default). This can be
     * set to 'strict', 'lax', 'none', or _true (which maps to 'strict').
     */
    private SameSite sameSite = SameSite._false;

    /**
     * a boolean indicating whether the cookie is only to be sent over HTTPS (false by default for HTTP,
     * true by default for HTTPS).
     */
    private boolean secure;

    /**
     * a boolean indicating whether the cookie is only to be sent over HTTPS (use this if you handle SSL
     * not in your jvm process).
     */
    private boolean secureProxy;

    /**
     * a boolean indicating whether the cookie is only to be sent over HTTP(S), and not made available
     * to client JavaScript (true by default).
     *
     * @default true
     */
    private boolean httpOnly = true;

    /**
     * a boolean indicating whether the cookie is to be signed (true by default). If this is true, another
     * cookie of the same name with the .sig suffix appended will also be sent, with a 27-byte url-safe
     * base64 SHA1 value representing the hash of cookie-name=cookie-value against the first Keygrip key.
     * This signature key is used to detect tampering the next time a cookie is received.
     */
    private boolean signed = true;

    /**
     * a boolean indicating whether to overwrite previously set cookies of the same name (true by default). If
     * this is true, all cookies set during the same request with the same name (regardless of path or domain)
     * are filtered out of the Set-Cookie header when setting this cookie.
     */
    private boolean overwrite;
}

