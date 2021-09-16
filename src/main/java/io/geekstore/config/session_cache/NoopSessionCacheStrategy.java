/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.session_cache;

/**
 * A cache that doesn't cache. The cache lookup will miss every time
 * so the session will always be taken from the database.
 *
 * Created on Nov, 2020 by @author bobo
 */
public class NoopSessionCacheStrategy implements SessionCacheStrategy {
    @Override
    public void set(CachedSession session) {
        // do nothing
    }

    @Override
    public CachedSession get(String sessionToken) {
        return null; // always null,
    }

    @Override
    public void delete(String sessionToken) {
        // do nothing
    }

    @Override
    public void clear() {
        // do nothing
    }
}
