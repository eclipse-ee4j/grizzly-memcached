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

package org.glassfish.grizzly.memcached.zookeeper;

/**
 * The interface using the ZooKeeper for synchronizing cache server list
 * <p>
 * Example of use:
 * {@code
 * final GrizzlyMemcachedCacheManager.Builder managerBuilder = new GrizzlyMemcachedCacheManager.Builder();
 * // setup zookeeper server
 * final ZooKeeperConfig zkConfig = ZooKeeperConfig.create("cache-manager", DEFAULT_ZOOKEEPER_ADDRESS);
 * zkConfig.setRootPath(ROOT);
 * zkConfig.setConnectTimeoutInMillis(3000);
 * zkConfig.setSessionTimeoutInMillis(30000);
 * zkConfig.setCommitDelayTimeInSecs(2);
 * managerBuilder.zooKeeperConfig(zkConfig);
 * // create a cache manager
 * final GrizzlyMemcachedCacheManager manager = managerBuilder.build();
 * final GrizzlyMemcachedCache.Builder<String, String> cacheBuilder = manager.createCacheBuilder("user");
 * // setup memcached servers
 * final Set<SocketAddress> memcachedServers = new HashSet<SocketAddress>();
 * memcachedServers.add(MEMCACHED_ADDRESS1);
 * memcachedServers.add(MEMCACHED_ADDRESS2);
 * cacheBuilder.servers(memcachedServers);
 * // create a user cache
 * final GrizzlyMemcachedCache<String, String> cache = cacheBuilder.build();
 * // ZooKeeperSupportCache's basic operations
 * if (cache.isZooKeeperSupported()) {
 * final String serverListPath = cache.getZooKeeperServerListPath();
 * final String serverList = cache.getCurrentServerListFromZooKeeper();
 * cache.setCurrentServerListOfZooKeeper("localhost:11211,localhost:11212");
 * }
 * // ...
 * // clean
 * manager.removeCache("user");
 * manager.shutdown();
 * }
 *
 * @author Bongjae Chang
 */
public interface ZooKeeperSupportCache {

    /**
     * Check if this cache supports the ZooKeeper for synchronizing the cache server list
     *
     * @return true if this cache supports it
     */
    public boolean isZooKeeperSupported();

    /**
     * Return the path of the cache server list which has been registered in the ZooKeeper server
     *
     * @return the path of the cache server list in the ZooKeeper server.
     *         "null" means this cache doesn't support the ZooKeeper or this cache is not started yet
     */
    public String getZooKeeperServerListPath();

    /**
     * Return the current cache server list string from the ZooKeeper server
     *
     * @return the current server list string
     */
    public String getCurrentServerListFromZooKeeper();

    /**
     * Set the current cache server list string with the given {@code cacheServerList}
     * <p>
     * {@code cacheServerList} could be comma separated host:port pairs, each corresponding to a memcached server.
     * e.g. "127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002"
     * Be careful that this operation will propagate {@code cacheServerList} to caches which has joinned the same cache name(scope)
     * because the cache list of ZooKeeper server will be changed.
     *
     * @param cacheServerList the cache server list string
     * @return true if this cache server list is set successfully
     */
    public boolean setCurrentServerListOfZooKeeper(final String cacheServerList);

    /**
     * Add the custom {@link BarrierListener}
     *
     * The given {@code listener} will be called after cache's default listener will be completed.
     * {@link BarrierListener#onInit} will be called when this cache will be registered in the ZooKeeper.
     * {@link BarrierListener#onCommit} will be called when this cache's server list will be changed in the ZooKeeper.
     * {@link BarrierListener#onDestroy} will be called when this cache will be unregistered in the ZooKeeper.
     * 
     * @param listener the custom listener
     */
    public void addZooKeeperListener( final BarrierListener listener );

    /**
     * Remove the custom {@link BarrierListener}
     *
     * The given {@code listener} will be called after cache's default listener will be completed.
     * {@link BarrierListener#onInit} will be called when this cache will be registered in the ZooKeeper.
     * {@link BarrierListener#onCommit} will be called when this cache's server list will be changed in the ZooKeeper.
     * {@link BarrierListener#onDestroy} will be called when this cache will be unregistered in the ZooKeeper.
     *
     * @param listener the custom listener which was given by {@link #addZooKeeperListener}
     */
    public void removeZooKeeperListener( final BarrierListener listener );
}
