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

import org.glassfish.grizzly.memcached.MemcachedCache;
import org.glassfish.grizzly.memcached.ValueWithCas;
import org.glassfish.grizzly.memcached.ValueWithKey;
import org.glassfish.grizzly.monitoring.MonitoringConfig;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Bongjae Chang
 */
public class CacheServerListBarrierListenerTest {
    private static final Logger logger = LoggerFactory.getLogger(CacheServerListBarrierListenerTest.class);

    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String ROOT = "/zktest";

    @Test
    public void testSimpleAddressParsing() {
        final String addressListString = "localhost:2222";
        final InetSocketAddress inetAddr = new InetSocketAddress("localhost", 2222);
        final Set<SocketAddress> addressSet = CacheServerListBarrierListener.getAddressesFromStringList(addressListString);
        Assert.assertNotNull(addressSet);
        Assert.assertEquals(1, addressSet.size());
        for (final SocketAddress addr : addressSet) {
            if (!(addr instanceof InetSocketAddress)) {
                Assert.fail("not InetSocketAddress");
            }
            final InetSocketAddress inetAddr2 = (InetSocketAddress) addr;
            Assert.assertEquals(inetAddr.getHostName(), inetAddr2.getHostName());
            Assert.assertEquals(inetAddr.getPort(), inetAddr2.getPort());
        }

        final String addressListString2 = CacheServerListBarrierListener.getStringListFromAddressSet(addressSet);
        Assert.assertEquals(addressListString, addressListString2);
    }

    @Test
    public void testAddressParsing() {
        final String addressListString = " localhost:2222, localhost:3333   ,localhost:4444 ";
        final Set<SocketAddress> addressSet = CacheServerListBarrierListener.getAddressesFromStringList(addressListString);
        Assert.assertNotNull(addressSet);
        Assert.assertEquals(3, addressSet.size());
        final String addressListString2 = CacheServerListBarrierListener.getStringListFromAddressSet(addressSet);
        Assert.assertNotNull(addressListString2);
        final Set<SocketAddress> addressSet2 = CacheServerListBarrierListener.getAddressesFromStringList(addressListString2);
        Assert.assertEquals(3, addressSet2.size());
        addressSet2.removeAll(addressSet);
        Assert.assertTrue(addressSet2.isEmpty());
    }

    // zookeeper server should be booted in local
    //@Test
    public void testCacheServerList() {
        final String serversString = "localhost:1111, localhost:2222, localhost:3333, localhost:4444";
        byte[] serversBytes = null;
        try {
            serversBytes = serversString.getBytes(DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException uee) {
            Assert.fail(uee.getMessage());
        }
        final Set<SocketAddress> initServers = CacheServerListBarrierListener.getAddressesFromStringList(serversString);
        final SimpleMemcachedCache cache = new SimpleMemcachedCache("user", initServers);
        final BarrierListener listener = new CacheServerListBarrierListener(cache, initServers);
        final String regionName = cache.getName();

        logger.info("##initial server = " + serversString);

        // init
        final ZKClient.Builder builder = new ZKClient.Builder("cache-server-list-test", DEFAULT_ZOOKEEPER_ADDRESS);
        builder.rootPath(ROOT).connectTimeoutInMillis(3000).sessionTimeoutInMillis(3000).commitDelayTimeInSecs(1);
        final ZKClient zkClient = builder.build();
        try {
            zkClient.connect();
        } catch (IOException ie) {
            logger.error("failed to connection the server", ie);
            Assert.fail();
        } catch (InterruptedException ignore) {
            Assert.fail();
        }

        // register the listener
        final String dataPath = zkClient.registerBarrier(regionName, listener, serversBytes);
        Assert.assertNotNull(dataPath);

        final String newServersString = "localhost:1111, localhost:3333, localhost:4444";
        byte[] newServersBytes = null;
        try {
            newServersBytes = newServersString.getBytes(DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException uee) {
            Assert.fail(uee.getMessage());
        }

        logger.info("##new server = " + newServersString);
        // modified the remote list
        zkClient.setData(dataPath, newServersBytes, -1);

        // wait for being updated
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            return;
        }

        logger.info("##current server = " + cache.getLocalServers());
        Assert.assertTrue(cache.isInServerList(new InetSocketAddress("localhost", 1111)));
        Assert.assertFalse(cache.isInServerList(new InetSocketAddress("localhost", 2222))); // removed
        Assert.assertTrue(cache.isInServerList(new InetSocketAddress("localhost", 3333)));
        Assert.assertTrue(cache.isInServerList(new InetSocketAddress("localhost", 4444)));

        final String newServersString2 = "localhost:1111, localhost:3333, localhost:4444, localhost:5555";
        byte[] newServersBytes2 = null;
        try {
            newServersBytes2 = newServersString2.getBytes(DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException uee) {
            Assert.fail(uee.getMessage());
        }

        logger.info("##new server = " + newServersString2);
        // modified the remote list
        zkClient.setData(dataPath, newServersBytes2, -1);

        // wait for being updated
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            return;
        }

        logger.info("##current server = " + cache.getLocalServers());
        Assert.assertTrue(cache.isInServerList(new InetSocketAddress("localhost", 1111)));
        Assert.assertFalse(cache.isInServerList(new InetSocketAddress("localhost", 2222))); // removed
        Assert.assertTrue(cache.isInServerList(new InetSocketAddress("localhost", 3333)));
        Assert.assertTrue(cache.isInServerList(new InetSocketAddress("localhost", 4444)));
        Assert.assertTrue(cache.isInServerList(new InetSocketAddress("localhost", 5555))); // added

        final String newServersString3 = "localhost:7777";
        byte[] newServersBytes3 = null;
        try {
            newServersBytes3 = newServersString3.getBytes(DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException uee) {
            Assert.fail(uee.getMessage());
        }

        logger.info("##new server = " + newServersString3);
        // modified the remote list
        zkClient.setData(dataPath, newServersBytes3, -1);

        // wait for being updated
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            return;
        }

        logger.info("##current server = " + cache.getLocalServers());
        Assert.assertFalse(cache.isInServerList(new InetSocketAddress("localhost", 1111))); // removed
        Assert.assertFalse(cache.isInServerList(new InetSocketAddress("localhost", 2222))); // removed
        Assert.assertFalse(cache.isInServerList(new InetSocketAddress("localhost", 3333))); // removed
        Assert.assertFalse(cache.isInServerList(new InetSocketAddress("localhost", 4444))); // removed
        Assert.assertFalse(cache.isInServerList(new InetSocketAddress("localhost", 5555))); // removed
        Assert.assertTrue(cache.isInServerList(new InetSocketAddress("localhost", 7777))); // added

        // clean
        zkClient.unregisterBarrier(regionName);
        clearRegionRepository(zkClient, regionName);
        clearBaseRepository(zkClient);
        zkClient.shutdown();
    }

    private static void clearRegionRepository(final ZKClient zkClient, final String regionName) {
        if (zkClient == null) {
            return;
        }
        final String regionPath = ROOT + "/barrier/" + regionName;
        final String dataPath = regionPath + "/data";
        final String currentPath = regionPath + "/current";
        final String participantsPath = regionPath + "/participants";

        if (zkClient.exists(dataPath, false) != null) {
            zkClient.delete(dataPath, -1);
        }
        if (zkClient.exists(currentPath, false) != null) {
            final List<String> currentNodes = zkClient.getChildren(currentPath, false);
            if (currentNodes == null || currentNodes.isEmpty()) {
                zkClient.delete(currentPath, -1);
            } else {
                for (String node : currentNodes) {
                    zkClient.delete(currentPath + "/" + node, -1);
                }
                zkClient.delete(currentPath, -1);
            }
        }
        if (zkClient.exists(participantsPath, false) != null) {
            final List<String> paticipants = zkClient.getChildren(participantsPath, false);
            if (paticipants == null || paticipants.isEmpty()) {
                zkClient.delete(participantsPath, -1);
            } else {
                for (String node : paticipants) {
                    zkClient.delete(participantsPath + "/" + node, -1);
                }
                zkClient.delete(participantsPath, -1);
            }
        }
        zkClient.delete(regionPath, -1);
    }

    private static void clearBaseRepository(final ZKClient zkClient) {
        zkClient.delete(ROOT + "/barrier", -1);
        zkClient.delete(ROOT, -1);
    }

    private static class SimpleMemcachedCache implements MemcachedCache {
        private final String cacheName;
        private final Set<SocketAddress> localCacheServerSet;

        private SimpleMemcachedCache(final String cacheName, final Set<SocketAddress> cacheServerSet) {
            this.cacheName = cacheName;
            this.localCacheServerSet = cacheServerSet;
        }

        private Set<SocketAddress> getLocalServers() {
            return localCacheServerSet;
        }

        @Override
        public boolean addServer(final SocketAddress serverAddress) {
            localCacheServerSet.add(serverAddress);
            return true;
        }

        @Override
        public void removeServer(final SocketAddress serverAddress) {
            localCacheServerSet.remove(serverAddress);
        }

        @Override
        public boolean isInServerList(final SocketAddress serverAddress) {
            return localCacheServerSet.contains(serverAddress);
        }

        @Override
        public List<SocketAddress> getCurrentServerList() {
            return new ArrayList<SocketAddress>(localCacheServerSet);
        }

        @Override
        public String getName() {
            return cacheName;
        }

        /**
         * empty methods
         */
        @Override
        public boolean set(Object key, Object value, int expirationInSecs, boolean noReply, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return false;
        }

        @Override
        public Map setMulti(Map map, int expirationInSecs) {
            return null;
        }

        @Override
        public Map setMulti(Map map, int expirationInSecs, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return null;
        }

        @Override
        public boolean add(Object key, Object value, int expirationInSecs, boolean noReply, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return false;
        }

        @Override
        public boolean replace(Object key, Object value, int expirationInSecs, boolean noReply, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return false;
        }

        @Override
        public boolean append(Object key, Object value, boolean noReply, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return false;
        }

        @Override
        public boolean prepend(Object key, Object value, boolean noReply, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return false;
        }

        @Override
        public boolean cas(Object key, Object value, int expirationInSecs, long cas, boolean noReply, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return false;
        }

        @Override
        public Map casMulti(Map map, int expirationInSecs) {
            return null;
        }

        @Override
        public Map casMulti(Map map, int expirationInSecs, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return null;
        }

        @Override
        public Object get(Object key, boolean noReply, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return null;
        }

        @Override
        public Map getMulti(Set keys) {
            return null;
        }

        @Override
        public Map getMulti(Set keys, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return null;
        }

        @Override
        public ValueWithKey getKey(Object key, boolean noReply, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return null;
        }

        @Override
        public ValueWithCas gets(Object key, boolean noReply, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return null;
        }

        @Override
        public Map getsMulti(Set keys) {
            return null;
        }

        @Override
        public Map getsMulti(Set keys, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return null;
        }

        @Override
        public Object gat(Object key, int expirationInSecs, boolean noReply, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return null;
        }

        @Override
        public boolean delete(Object key, boolean noReply, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return false;
        }

        @Override
        public Map deleteMulti(Set keys) {
            return null;
        }

        @Override
        public Map deleteMulti(Set keys, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return null;
        }

        @Override
        public long incr(Object key, long delta, long initial, int expirationInSecs, boolean noReply, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return 0;
        }

        @Override
        public long decr(Object key, long delta, long initial, int expirationInSecs, boolean noReply, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return 0;
        }

        @Override
        public String saslAuth(SocketAddress address, String mechanism, byte[] data, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return null;
        }

        @Override
        public String saslStep(SocketAddress address, String mechanism, byte[] data, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return null;
        }

        @Override
        public String saslList(SocketAddress address, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return null;
        }

        @Override
        public Map<String, String> stats(SocketAddress address, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return null;
        }

        @Override
        public Map<String, String> statsItems(SocketAddress address, String item, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return null;
        }

        @Override
        public boolean quit(SocketAddress address, boolean noReply, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return false;
        }

        @Override
        public boolean flushAll(SocketAddress address, int expirationInSecs, boolean noReply, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return false;
        }

        @Override
        public boolean touch(Object key, int expirationInSecs, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return false;
        }

        @Override
        public boolean noop(SocketAddress address, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return false;
        }

        @Override
        public boolean verbosity(SocketAddress address, int verbosity, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return false;
        }

        @Override
        public String version(SocketAddress address, long writeTimeoutInMillis, long responseTimeoutInMillis) {
            return null;
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean set(Object key, Object value, int expirationInSecs, boolean noReply) {
            return false;
        }

        @Override
        public boolean add(Object key, Object value, int expirationInSecs, boolean noReply) {
            return false;
        }

        @Override
        public boolean replace(Object key, Object value, int expirationInSecs, boolean noReply) {
            return false;
        }

        @Override
        public boolean append(Object key, Object value, boolean noReply) {
            return false;
        }

        @Override
        public boolean prepend(Object key, Object value, boolean noReply) {
            return false;
        }

        @Override
        public boolean cas(Object key, Object value, int expirationInSecs, long cas, boolean noReplys) {
            return false;
        }

        @Override
        public Object get(Object key, boolean noReply) {
            return null;
        }

        @Override
        public ValueWithKey getKey(Object key, boolean noReply) {
            return null;
        }

        @Override
        public ValueWithCas gets(Object key, boolean noReply) {
            return null;
        }

        @Override
        public Object gat(Object key, int expirationInSecs, boolean noReplys) {
            return null;
        }

        @Override
        public boolean delete(Object key, boolean noReply) {
            return false;
        }

        @Override
        public long incr(Object key, long delta, long initial, int expirationInSecs, boolean noReply) {
            return 0;
        }

        @Override
        public long decr(Object key, long delta, long initial, int expirationInSecs, boolean noReply) {
            return 0;
        }

        @Override
        public String saslAuth(SocketAddress address, String mechanism, byte[] data) {
            return null;
        }

        @Override
        public String saslStep(SocketAddress address, String mechanism, byte[] data) {
            return null;
        }

        @Override
        public String saslList(SocketAddress address) {
            return null;
        }

        @Override
        public Map<String, String> stats(SocketAddress address) {
            return null;
        }

        @Override
        public Map<String, String> statsItems(SocketAddress address, String item) {
            return null;
        }

        @Override
        public boolean quit(SocketAddress address, boolean noReply) {
            return false;
        }

        @Override
        public boolean flushAll(SocketAddress address, int expirationInSecs, boolean noReply) {
            return false;
        }

        @Override
        public boolean touch(Object key, int expirationInSecs) {
            return false;
        }

        @Override
        public boolean noop(SocketAddress addresss) {
            return false;
        }

        @Override
        public boolean verbosity(SocketAddress address, int verbosity) {
            return false;
        }

        @Override
        public String version(SocketAddress address) {
            return null;
        }

        @Override
        public MonitoringConfig getMonitoringConfig() {
            return null;
        }
    }
}
