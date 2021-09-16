/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.session_cache;

import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Created on Nov, 2020 by @author bobo
 */
@RequiredArgsConstructor
public class TestingSessionCacheStrategy implements SessionCacheStrategy {

    private final Map<String, CachedSession> theSessionCache;

    @Override
    public void set(CachedSession session) {
        theSessionCache.put(session.getToken(), session);
    }

    @Override
    public CachedSession get(String sessionToken) {
        return theSessionCache.get(sessionToken);
    }

    @Override
    public void delete(String sessionToken) {
        theSessionCache.remove(sessionToken);
    }

    @Override
    public void clear() {
        theSessionCache.clear();
    }
}
