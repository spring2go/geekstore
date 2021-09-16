/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.session_cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import static io.geekstore.common.Constant.DEFAULT_IN_MEMORY_CACHE_SIZE;

/**
 * Caches session in memory, using a LRU cache implementation. Not suitable for
 * multi-server setups since the cache will be local to each instance, reducing
 * its effectiveness. By default the cache has a size of 1000, meaning that after
 * 1000 sessions have been cached, any new sessions will cause the least-recently-used
 * session to be evicted (removed) from the cache.
 *
 * The cache size can be configured by passing a different number to the constructor
 * function.
 *
 * Created on Nov, 2020 by @author bobo
 */
public class InMemorySessionCacheStrategy implements SessionCacheStrategy {
    private final Cache<String, CachedSession> cache;
    private final int cacheSize;

    public InMemorySessionCacheStrategy() {
        this(DEFAULT_IN_MEMORY_CACHE_SIZE);
    }

    public InMemorySessionCacheStrategy(int cacheSize) {
        if (cacheSize < 1) {
            throw new RuntimeException("cacheSize must be a positive integer");
        }
        this.cacheSize = cacheSize;
        cache = CacheBuilder.newBuilder().maximumSize(this.cacheSize).build();
    }

    @Override
    public void set(CachedSession session) {
        this.cache.put(session.getToken(), session);
    }

    @Override
    public CachedSession get(String sessionToken) {
        return this.cache.getIfPresent(sessionToken);
    }

    @Override
    public void delete(String sessionToken) {
        this.cache.invalidate(sessionToken);
    }

    @Override
    public void clear() {
        this.cache.cleanUp();
    }
}
