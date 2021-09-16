/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.session_cache;

import lombok.Data;

import java.util.Date;

/**
 * A simplified representation of a Session which is easy to store.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class CachedSession {
    /**
     * The timestamp after which this cache entry is considered stale and
     * a fresh copy of the data will be set. Based on the `sessionCacheTTL` option.
     */
    private long cacheExpiry;
    private Long id;
    private String token;
    private Date expires;
    private Long activeOrderId;
    private String authenticationStrategy;
    private CachedSessionUser user;
}
