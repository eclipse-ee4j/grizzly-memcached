/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.glassfish.grizzly.memcached.GrizzlyMemcachedCache;
import org.glassfish.grizzly.memcached.GrizzlyMemcachedCacheManager;
import org.glassfish.grizzly.memcached.MemcachedCache;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Bongjae Chang
 */
public class PreferRemoteConfigTest {

    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final String DEFAULT_LOCAL_HOST = "localhost";
    private static final String ROOT = "/zktest";
    private static final String BASE_PATH = "/barrier";
    private static final String DATA_PATH = "/data";
    private static final byte[] NO_DATA = new byte[0];

    private static final String DEFAULT_CHARSET = "UTF-8";

    @Test
    public void testWithoutZooKeeperConfig() {
        final GrizzlyMemcachedCacheManager manager = new GrizzlyMemcachedCacheManager.Builder().build();
        try {
            final GrizzlyMemcachedCache.Builder<String, String> builder = manager.createCacheBuilder("user");
            final Set<SocketAddress> serverList = new HashSet<SocketAddress>();
            final SocketAddress local = new InetSocketAddress(DEFAULT_LOCAL_HOST, 11211);
            serverList.add(local);
            builder.servers(serverList);
            builder.preferRemoteConfig(true);
            final MemcachedCache<String, String> userCache = builder.build();
            // if zookeeper config is not set, local config is used
            assertTrue(userCache.isInServerList(local));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testWithoutZooKeeperOrRemoteConfig() {
        final GrizzlyMemcachedCacheManager.Builder managerBuilder = new GrizzlyMemcachedCacheManager.Builder();
        // setup zookeeper server
        final ZooKeeperConfig zkConfig = ZooKeeperConfig.create("cache-manager", DEFAULT_ZOOKEEPER_ADDRESS);
        zkConfig.setRootPath(ROOT);
        zkConfig.setConnectTimeoutInMillis(3000);
        zkConfig.setSessionTimeoutInMillis(30000);
        zkConfig.setCommitDelayTimeInSecs(2);
        managerBuilder.zooKeeperConfig(zkConfig);
        // create a cache manager
        final GrizzlyMemcachedCacheManager manager = managerBuilder.build();
        final ZKClient zkClient = createZKClient();
        try {
            final GrizzlyMemcachedCache.Builder<String, String> builder = manager.createCacheBuilder("user");
            final Set<SocketAddress> serverList = new HashSet<SocketAddress>();
            final SocketAddress local = new InetSocketAddress(DEFAULT_LOCAL_HOST, 11211);
            serverList.add(local);
            builder.servers(serverList);
            builder.preferRemoteConfig(true);
            final MemcachedCache<String, String> userCache = builder.build();
            if (zkClient == null) {
                // if zookeeper is not booted, local config is used
                assertTrue(userCache.isInServerList(local));
            } else {
                // if zookeeper is booted, assumes that the remote config is not setup
                assertFalse(userCache.isInServerList(local));
            }
        } finally {
            manager.shutdown();
            clearTestRepository(zkClient);
        }
    }

    // zookeeper server should be booted in local
    //@Test
    public void testWithRemoteConfig() {
        // setup remote config
        final ZKClient zkClient = createZKClient();
        Assert.assertNotNull(zkClient);
        createWhenThereIsNoNode(zkClient, ROOT, NO_DATA, CreateMode.PERSISTENT);
        createWhenThereIsNoNode(zkClient, ROOT + BASE_PATH, NO_DATA, CreateMode.PERSISTENT);
        createWhenThereIsNoNode(zkClient, ROOT + BASE_PATH + "/user", NO_DATA, CreateMode.PERSISTENT);
        createWhenThereIsNoNode(zkClient, ROOT + BASE_PATH + "/user" + DATA_PATH, NO_DATA, CreateMode.PERSISTENT);
        final String cacheServerList = DEFAULT_LOCAL_HOST + ":11211";
        byte[] serverListBytes = null;
        try {
            serverListBytes = cacheServerList.getBytes(DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException uee) {
            Assert.fail(uee.getMessage());
        }
        zkClient.setData(ROOT + BASE_PATH + "/user" + DATA_PATH, serverListBytes, 0);

        final GrizzlyMemcachedCacheManager.Builder managerBuilder = new GrizzlyMemcachedCacheManager.Builder();
        // setup zookeeper server
        final ZooKeeperConfig zkConfig = ZooKeeperConfig.create("cache-manager", DEFAULT_ZOOKEEPER_ADDRESS);
        zkConfig.setRootPath(ROOT);
        zkConfig.setConnectTimeoutInMillis(3000);
        zkConfig.setSessionTimeoutInMillis(30000);
        zkConfig.setCommitDelayTimeInSecs(2);
        managerBuilder.zooKeeperConfig(zkConfig);
        // create a cache manager
        final GrizzlyMemcachedCacheManager manager = managerBuilder.build();
        try {
            final GrizzlyMemcachedCache.Builder<String, String> builder = manager.createCacheBuilder("user");
            final SocketAddress local = new InetSocketAddress(DEFAULT_LOCAL_HOST, 11211);
            builder.preferRemoteConfig(true);
            final MemcachedCache<String, String> userCache = builder.build();
            assertTrue(userCache.isInServerList(local));
        } finally {
            manager.shutdown();
            clearTestRepository(zkClient);
        }
    }

    private static ZKClient createZKClient() {
        final ZKClient.Builder zkBuilder = new ZKClient.Builder("test-zk-client", DEFAULT_ZOOKEEPER_ADDRESS);
        zkBuilder.rootPath(ROOT).connectTimeoutInMillis(3000).sessionTimeoutInMillis(3000).commitDelayTimeInSecs(30);
        final ZKClient zkClient = zkBuilder.build();
        try {
            if (!zkClient.connect()) {
                return null;
            }
        } catch (IOException ie) {
            return null;
        } catch (InterruptedException ignore) {
            return null;
        }
        return zkClient;
    }

    private static boolean createWhenThereIsNoNode(final ZKClient zkClient, final String path, final byte[] data, final CreateMode createMode) {
        if (zkClient == null) {
            return false;
        }
        if (zkClient.exists(path, false) != null) {
            return false;
        }
        zkClient.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
        return true;
    }

    private static void clearTestRepository(final ZKClient zkClient) {
        if (zkClient == null) {
            return;
        }
        zkClient.delete(ROOT + BASE_PATH + "/user/participants", -1);
        zkClient.delete(ROOT + BASE_PATH + "/user/current", -1);
        zkClient.delete(ROOT + BASE_PATH + "/user" + DATA_PATH, -1);
        zkClient.delete(ROOT + BASE_PATH + "/user", -1);
        zkClient.delete(ROOT + BASE_PATH, -1);
        zkClient.delete(ROOT, -1);
        zkClient.shutdown();
    }
}
