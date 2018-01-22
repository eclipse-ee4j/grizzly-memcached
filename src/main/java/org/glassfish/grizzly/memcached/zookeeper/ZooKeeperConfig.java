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

import java.io.Serializable;

/**
 * The configuration for ZooKeeper client
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
 * // ...
 * // clean
 * manager.removeCache("user");
 * manager.shutdown();
 * }
 *
 * @author Bongjae Chang
 */
public class ZooKeeperConfig implements Serializable {

    private static final long serialVersionUID = -3100430916673953287L;

    private static final String DEFAULT_ROOT_PATH = "/";
    private static final long DEFAULT_CONNECT_TIMEOUT_IN_MILLIS = 5000; // 5secs
    private static final long DEFAULT_SESSION_TIMEOUT_IN_MILLIS = 30000; // 30secs
    private static final long DEFAULT_COMMIT_DELAY_TIME_IN_SECS = 60; // 60secs

    private final String name;
    private final String zooKeeperServerList;

    private String rootPath = DEFAULT_ROOT_PATH;
    private long connectTimeoutInMillis = DEFAULT_CONNECT_TIMEOUT_IN_MILLIS;
    private long sessionTimeoutInMillis = DEFAULT_SESSION_TIMEOUT_IN_MILLIS;
    private long commitDelayTimeInSecs = DEFAULT_COMMIT_DELAY_TIME_IN_SECS;

    /**
     * Create ZKClient's configuration with the specific name or Id
     *
     * @param name                name or id
     * @param zooKeeperServerList comma separated host:port pairs, each corresponding to a zookeeper server.
     *                            e.g. "127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002"
     * @return ZKClient's configuration
     */
    public static ZooKeeperConfig create(final String name, final String zooKeeperServerList) {
        return new ZooKeeperConfig(name, zooKeeperServerList);
    }


    private ZooKeeperConfig(final String name, final String zooKeeperServerList) {
        this.name = name;
        this.zooKeeperServerList = zooKeeperServerList;
    }

    /**
     * Root path for ZKClient
     *
     * @param rootPath root path of the zookeeper. default is "/".
     */
    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * Connect timeout in milli-seconds
     *
     * @param connectTimeoutInMillis connect timeout. negative value means "never timed out". default is 5000(5 secs).
     */
    public void setConnectTimeoutInMillis(final long connectTimeoutInMillis) {
        this.connectTimeoutInMillis = connectTimeoutInMillis;
    }

    /**
     * Session timeout in milli-seconds
     *
     * @param sessionTimeoutInMillis Zookeeper connection's timeout. default is 30000(30 secs).
     */
    public void setSessionTimeoutInMillis(final long sessionTimeoutInMillis) {
        this.sessionTimeoutInMillis = sessionTimeoutInMillis;
    }

    /**
     * Delay time in seconds for committing
     *
     * @param commitDelayTimeInSecs delay time before committing. default is 60(60secs).
     */
    public void setCommitDelayTimeInSecs(final long commitDelayTimeInSecs) {
        this.commitDelayTimeInSecs = commitDelayTimeInSecs;
    }

    public String getName() {
        return name;
    }

    public String getZooKeeperServerList() {
        return zooKeeperServerList;
    }

    public String getRootPath() {
        return rootPath;
    }

    public long getConnectTimeoutInMillis() {
        return connectTimeoutInMillis;
    }

    public long getSessionTimeoutInMillis() {
        return sessionTimeoutInMillis;
    }

    public long getCommitDelayTimeInSecs() {
        return commitDelayTimeInSecs;
    }

    @Override
    public String toString() {
        return "ZooKeeperConfig{" +
                "name='" + name + '\'' +
                ", zooKeeperServerList='" + zooKeeperServerList + '\'' +
                ", rootPath='" + rootPath + '\'' +
                ", connectTimeoutInMillis=" + connectTimeoutInMillis +
                ", sessionTimeoutInMillis=" + sessionTimeoutInMillis +
                ", commitDelayTimeInSecs=" + commitDelayTimeInSecs +
                '}';
    }
}
