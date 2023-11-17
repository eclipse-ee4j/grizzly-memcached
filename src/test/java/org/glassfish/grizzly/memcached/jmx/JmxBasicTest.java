/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.memcached.jmx;

import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;
import org.glassfish.grizzly.memcached.GrizzlyMemcachedCacheManager;
import org.glassfish.grizzly.memcached.MemcachedCache;
import org.glassfish.grizzly.memcached.pool.BaseObjectPool;
import org.glassfish.grizzly.memcached.pool.ObjectPool;
import org.glassfish.grizzly.memcached.pool.PoolableObjectFactory;
import org.glassfish.grizzly.monitoring.jmx.JmxObject;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Bongjae Chang
 */
public class JmxBasicTest {

    /*
    @Test
    public void testMy() {
        final GrizzlyMemcachedCacheManager cacheManager = new GrizzlyMemcachedCacheManager.Builder().build();

        final org.glassfish.grizzly.memcached.GrizzlyMemcachedCache.Builder<String, String> builder1 = cacheManager.createCacheBuilder("JmxGrizzlyMemcachedCache1");
        builder1.jmxEnabled(true);
        builder1.healthMonitorIntervalInSecs(3L); ///// todo test
        final MemcachedCache<String, String> testCache1 = builder1.build();
        testCache1.addServer(new InetSocketAddress(11211));
        testCache1.addServer(new InetSocketAddress(11212));
        testCache1.addServer(new InetSocketAddress(11213));
        final org.glassfish.grizzly.memcached.GrizzlyMemcachedCache.Builder<String, String> builder2 = cacheManager.createCacheBuilder("JmxGrizzlyMemcachedCache2");
        builder2.jmxEnabled(true);
        builder2.healthMonitorIntervalInSecs(33L); ///// todo test
        final MemcachedCache<String, String> testCache2 = builder2.build();
        testCache2.addServer(new InetSocketAddress(11211));
        testCache2.addServer(new InetSocketAddress(11212));
        testCache2.addServer(new InetSocketAddress(11213));


        for (int i=0 ; i<1000; i++) {
            // todo test
            System.out.println(testCache1.add("name", "foo", 60, false));
            System.out.println(testCache1.get("name", false));
            try {
                TimeUnit.SECONDS.sleep(5L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            TimeUnit.SECONDS.sleep(6300L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        cacheManager.shutdown();

        try {
            TimeUnit.SECONDS.sleep(20L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    */

    @Test
    public void testJmxGrizzlyMemcachedCache() {
        final GrizzlyMemcachedCacheManager cacheManager = new GrizzlyMemcachedCacheManager.Builder().build();
        cacheManager.createCacheBuilder("JmxGrizzlyMemcachedCache").jmxEnabled(true).build();

        // You can monitor this cache client via JMX before shutdown.
        cacheManager.shutdown();
    }

    @Test
    public void testGrizzlyMemcachedCache() {
        final GrizzlyJmxManager jmxManager = GrizzlyJmxManager.instance();

        final GrizzlyMemcachedCacheManager cacheManager = new GrizzlyMemcachedCacheManager.Builder().build();
        final MemcachedCache<String, String> testCache =
                cacheManager.<String, String>createCacheBuilder("JmxGrizzlyMemcachedCache").build();
        final JmxObject jmxPoolObject = (JmxObject) testCache.getMonitoringConfig().createManagementObject();
        assertNotNull(jmxPoolObject);
        jmxManager.registerAtRoot(jmxPoolObject);

        // You can monitor this cache client via JMX before shutdown.
        cacheManager.shutdown();
        jmxManager.deregister(jmxPoolObject);
    }

    @Test
    public void testBaseObjectPool() {
        final BaseObjectPool.Builder<Integer, Integer> builder =
                new BaseObjectPool.Builder<>(new PoolableObjectFactory<Integer, Integer>() {
                    private int id = 0;

                    @Override
                    public Integer createObject(Integer key) throws Exception {
                        return ++id;
                    }

                    @Override
                    public void destroyObject(Integer key, Integer value) throws Exception {
                    }

                    @Override
                    public boolean validateObject(Integer key, Integer value) throws Exception {
                        return true;
                    }
                });
        builder.name("JmxObjectPoolTest");
        builder.max(20);
        builder.min(10);
        builder.disposable(false);
        builder.keepAliveTimeoutInSecs(-1);
        final ObjectPool<Integer, Integer> pool = builder.build();

        final GrizzlyJmxManager jmxManager = GrizzlyJmxManager.instance();
        final JmxObject jmxPoolObject = (JmxObject) pool.getMonitoringConfig().createManagementObject();
        assertNotNull(jmxPoolObject);
        jmxManager.registerAtRoot(jmxPoolObject);

        final int poolCount = 5;
        final int borrowObjCount = 15;
        for (int i = 0; i < poolCount; i++) {
            try {
                pool.createAllMinObjects(i);
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
            // ensure min 10
            Assert.assertEquals(10, pool.getPoolSize(i));
            Assert.assertEquals(0, pool.getActiveCount(i));
            Assert.assertEquals(10, pool.getIdleCount(i));
            Assert.assertEquals(10, pool.getPeakCount(i));

            final Integer[] objects = new Integer[borrowObjCount];
            for (int j = 0; j < borrowObjCount; j++) {
                try {
                    objects[j] = pool.borrowObject(i, -1);
                } catch (Exception e) {
                    Assert.fail(e.getMessage());
                }
                assertNotNull(objects[j]);
            }
            // 10 object in pool, 5 new object
            Assert.assertEquals(borrowObjCount, pool.getPoolSize(i));
            Assert.assertEquals(borrowObjCount, pool.getActiveCount(i));
            Assert.assertEquals(0, pool.getIdleCount(i));
            Assert.assertEquals(borrowObjCount, pool.getPeakCount(i));

            for (int j = 0; j < borrowObjCount; j++) {
                try {
                    pool.returnObject(i, objects[j]);
                } catch (Exception e) {
                    Assert.fail(e.getMessage());
                }
            }

            Assert.assertEquals(borrowObjCount, pool.getPoolSize(i));
            Assert.assertEquals(0, pool.getActiveCount(i));
            Assert.assertEquals(borrowObjCount, pool.getIdleCount(i));
            Assert.assertEquals(borrowObjCount, pool.getPeakCount(i));
        }
        Assert.assertEquals(poolCount * borrowObjCount, pool.getTotalPoolSize());
        Assert.assertEquals(borrowObjCount, pool.getHighestPeakCount());
        Assert.assertEquals(0, pool.getTotalActiveCount());
        Assert.assertEquals(poolCount * borrowObjCount, pool.getTotalIdleCount());

        pool.destroy();
        jmxManager.deregister(jmxPoolObject);
    }
}
