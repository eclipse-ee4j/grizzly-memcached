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

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author Bongjae Chang
 */
public class FailoverTest {

    private static final Logger logger = LoggerFactory.getLogger(BasicCommandTest.class);

    private final static SocketAddress DEFAULT_MEMCACHED_ADDRESS = new InetSocketAddress(11211);
    private final static SocketAddress FAILOVER_MEMCACHED_ADDRESS = new InetSocketAddress(11212);
    private final static SocketAddress MOCK_MEMCACHED_ADDRESS = new InetSocketAddress(21212);

    // this is manual test
    //@Test
    public void testBasicFailover() {
        final GrizzlyMemcachedCacheManager manager = new GrizzlyMemcachedCacheManager.Builder().build();
        final GrizzlyMemcachedCache.Builder<String, String> builder = manager.createCacheBuilder("user");
        builder.healthMonitorIntervalInSecs(3);
        final MemcachedCache<String, String> userCache = builder.build();
        userCache.addServer(DEFAULT_MEMCACHED_ADDRESS);

        // test alive
        Assert.assertTrue(userCache.isInServerList(DEFAULT_MEMCACHED_ADDRESS));
        Assert.assertTrue(userCache.set("name", "foo", 0, false));
        Assert.assertEquals("foo", userCache.get("name", false));

        logger.info("please stop the memcached server in 7 secs...");
        try {
            Thread.sleep(7 * 1000);
        } catch (InterruptedException ignore) {
        }

        // test failure
        Assert.assertFalse(userCache.set("name", "foo", 0, false));
        Assert.assertFalse(userCache.isInServerList(DEFAULT_MEMCACHED_ADDRESS));

        logger.info("please start the memcached server in 7 secs...");
        try {
            Thread.sleep(7 * 1000);
        } catch (InterruptedException ignore) {
        }

        // test revival
        Assert.assertTrue(userCache.isInServerList(DEFAULT_MEMCACHED_ADDRESS));
        Assert.assertTrue(userCache.set("name", "foo", 0, false));
        Assert.assertEquals("foo", userCache.get("name", false));
        Assert.assertTrue(userCache.delete("name", false));

        manager.shutdown();
    }

    // this is manual test
    //@Test
    public void testNoRunningServer() {
        final GrizzlyMemcachedCacheManager manager = new GrizzlyMemcachedCacheManager.Builder().build();
        final GrizzlyMemcachedCache.Builder<String, String> builder = manager.createCacheBuilder("user");
        builder.healthMonitorIntervalInSecs(3);
        final MemcachedCache<String, String> userCache = builder.build();
        userCache.addServer(DEFAULT_MEMCACHED_ADDRESS);

        // test failure
        Assert.assertTrue(userCache.isInServerList(DEFAULT_MEMCACHED_ADDRESS));
        Assert.assertFalse(userCache.set("name", "foo", 0, false));
        Assert.assertNull(userCache.get("name", false));

        logger.info("please start the memcached server in 7 secs...");
        try {
            Thread.sleep(7 * 1000);
        } catch (InterruptedException ignore) {
        }

        // test revival
        Assert.assertTrue(userCache.isInServerList(DEFAULT_MEMCACHED_ADDRESS));
        Assert.assertTrue(userCache.set("name", "foo", 0, false));
        Assert.assertEquals("foo", userCache.get("name", false));
        Assert.assertTrue(userCache.delete("name", false));

        logger.info("please stop the memcached server in 7 secs...");
        try {
            Thread.sleep(7 * 1000);
        } catch (InterruptedException ignore) {
        }

        Assert.assertFalse(userCache.set("name", "foo", 0, false));

        // test failure
        Assert.assertFalse(userCache.isInServerList(DEFAULT_MEMCACHED_ADDRESS));

        manager.shutdown();
    }

    // this is manual test
    //@Test
    public void testFailoverAndFailback() {
        final SocketAddress mainServerForNameKey = FAILOVER_MEMCACHED_ADDRESS;
        final SocketAddress failoverServerForNameKey = DEFAULT_MEMCACHED_ADDRESS;

        final GrizzlyMemcachedCacheManager manager = new GrizzlyMemcachedCacheManager.Builder().build();
        final GrizzlyMemcachedCache.Builder<String, String> builder = manager.createCacheBuilder("user");
        builder.healthMonitorIntervalInSecs(3);
        final MemcachedCache<String, String> userCache = builder.build();
        userCache.addServer(mainServerForNameKey);
        userCache.addServer(failoverServerForNameKey);

        Assert.assertTrue(userCache.isInServerList(mainServerForNameKey));
        Assert.assertTrue(userCache.isInServerList(failoverServerForNameKey));

        // "name" key stored mainServerForNameKey address
        Assert.assertTrue(userCache.set("name", "foo", 0, false));
        Assert.assertEquals("foo", userCache.get("name", false));

        logger.info("please stop the memcached server({}) in 7 secs...", mainServerForNameKey);
        try {
            Thread.sleep(7 * 1000);
        } catch (InterruptedException ignore) {
        }

        // cache missed
        Assert.assertFalse(userCache.set("name", "bar", 0, false));

        Assert.assertFalse(userCache.isInServerList(mainServerForNameKey));
        Assert.assertTrue(userCache.isInServerList(failoverServerForNameKey));

        // stored in failoverServerForNameKey address
        Assert.assertTrue(userCache.add("name", "bar", 0, false));
        Assert.assertEquals("bar", userCache.get("name", false));

        logger.info("please start the memcached server({}) in 7 secs...", mainServerForNameKey);
        try {
            Thread.sleep(7 * 1000);
        } catch (InterruptedException ignore) {
        }

        // test revival
        Assert.assertTrue(userCache.isInServerList(mainServerForNameKey));
        Assert.assertTrue(userCache.isInServerList(failoverServerForNameKey));

        Assert.assertTrue(userCache.add("name", "foo", 0, false));
        Assert.assertEquals("foo", userCache.get("name", false));
        Assert.assertTrue(userCache.delete("name", false));

        logger.info("please stop the memcached server({}) in 7 secs...", mainServerForNameKey);
        try {
            Thread.sleep(7 * 1000);
        } catch (InterruptedException ignore) {
        }

        Assert.assertNull(userCache.get("name", false));
        Assert.assertFalse(userCache.isInServerList(mainServerForNameKey));
        Assert.assertTrue(userCache.isInServerList(failoverServerForNameKey));

        Assert.assertEquals("bar", userCache.get("name", false));
        Assert.assertTrue(userCache.delete("name", false));

        manager.shutdown();
    }

    @Test
    public void testNoRunningWithMockServer() {
        final GrizzlyMemcachedCacheManager manager = new GrizzlyMemcachedCacheManager.Builder().build();
        final GrizzlyMemcachedCache.Builder<String, String> builder = manager.createCacheBuilder("user");
        builder.responseTimeoutInMillis(3000);
        builder.healthMonitorIntervalInSecs(1);
        final MemcachedCache<String, String> userCache = builder.build();
        userCache.addServer(MOCK_MEMCACHED_ADDRESS);

        // test failure
        Assert.assertNull(userCache.version(MOCK_MEMCACHED_ADDRESS));
        Assert.assertFalse(userCache.isInServerList(MOCK_MEMCACHED_ADDRESS));

        final TCPNIOTransport transport;
        final FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter()).add(new MemcachedServerVersionFilter());
        transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());
        try {
            transport.bind(MOCK_MEMCACHED_ADDRESS);
        } catch (IOException e) {
            Assert.fail("failed to bind the transport");
        }
        try {
            transport.start();
        } catch (IOException ie) {
            logger.error("failed to start the transport", ie);
        }

        // wait for recovering the server
        logger.info("please wait for recovering the server");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignore) {
        }

        // test revival
        Assert.assertTrue(userCache.isInServerList(MOCK_MEMCACHED_ADDRESS));
        Assert.assertNotNull(userCache.version(MOCK_MEMCACHED_ADDRESS));

        try {
            transport.stop();
        } catch (IOException ignore) {
        }

        // test failure
        Assert.assertNull(userCache.version(MOCK_MEMCACHED_ADDRESS));
        Assert.assertNull(userCache.version(MOCK_MEMCACHED_ADDRESS));
        Assert.assertFalse(userCache.isInServerList(MOCK_MEMCACHED_ADDRESS));

        manager.shutdown();
    }

    private static class MemcachedServerVersionFilter extends BaseFilter {
        private static final byte[] VERSION_BYTES = "1.4.10".getBytes();

        public NextAction handleRead(FilterChainContext ctx) throws IOException {
            final Buffer buffer = MemoryManager.DEFAULT_MEMORY_MANAGER.allocate(30);
            buffer.allowBufferDispose(true);
            buffer.put((byte) 0x81); // response magic
            buffer.put((byte) 0x0b); // version opcode
            buffer.putShort((short) 0); // key length
            buffer.put((byte) 0); // extra length
            buffer.put((byte) 0); // data type
            buffer.putShort((short) 0); // status ok
            buffer.putInt(VERSION_BYTES.length); // total body length
            buffer.putInt(0); // opaque
            buffer.putLong(0L); // cas
            buffer.put(VERSION_BYTES);
            buffer.flip();
            ctx.write(buffer);
            return ctx.getStopAction();
        }
    }
}
