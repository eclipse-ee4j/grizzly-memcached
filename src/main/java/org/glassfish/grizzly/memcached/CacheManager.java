/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.grizzly.memcached;

/**
 * The interface for managing caches based on JSR-107(JCache)
 *
 * @author Bongjae Chang
 */
public interface CacheManager {
    /**
     * Creates a new {@link CacheBuilder} for the named cache to be managed by this cache manager.
     * <p>
     * The returned CacheBuilder is associated with this CacheManager.
     * The Cache will be created, added to the caches controlled by this CacheManager and started when
     * {@link CacheBuilder#build()} is called.
     *
     * @param cacheName the name of the cache to build. A cache name must consist of at least one non-whitespace character.
     * @return the CacheBuilder for the named cache
     */
    public <K, V> CacheBuilder<K, V> createCacheBuilder(final String cacheName);

    /**
     * Looks up a named cache.
     *
     * @param cacheName the name of the cache to look for
     * @return the Cache or null if it does exist
     */
    public <K, V> Cache<K, V> getCache(final String cacheName);

    /**
     * Remove a cache from the CacheManager. The cache will be stopped.
     *
     * @param cacheName the cache name
     * @return true if the cache was removed
     */
    public boolean removeCache(final String cacheName);

    /**
     * Shuts down the CacheManager.
     */
    public void shutdown();
}
