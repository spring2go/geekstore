/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.session_cache;

/**
 * This strategy defines how sessions get cached. Since most requests will need the Session
 * object for permissions data, it can become a bottleneck to go to the database and do a multi-join
 * SQL query each time. Therefore we cache the session data only perform the SQL query once and upon
 * invalidation of the cache.
 *
 * Created on Nov, 2020 by @author bobo
 */
public interface SessionCacheStrategy {
    /**
     * Store the session in the cache. When caching a session, the data
     * should not be modified apart from performing any transforms needed to
     * get it into a state to be stored.
     */
    void set(CachedSession session);
    /**
     * Retrieve the session from the cache
     */
    CachedSession get(String sessionToken);
    /**
     * Delete a session from the cache
     */
    void delete(String sessionToken);
    /**
     * Clear the entire cache
     */
    void clear();
}
