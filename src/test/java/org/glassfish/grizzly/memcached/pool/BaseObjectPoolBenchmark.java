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

package org.glassfish.grizzly.memcached.pool;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Bongjae Chang
 */
public class BaseObjectPoolBenchmark {

    private static final Logger logger = LoggerFactory.getLogger(BaseObjectPoolBenchmark.class);

    @Test
    public void testBenchmarking() {
        final int loop = 2 * 1000 * 1000;
        final int poolSize = 150;

        final KeyedPoolableObjectFactory<String, String> apacheFactory = new KeyedPoolableObjectFactory<String, String>() {
            private static final String VALUE_NAME = "value";
            private int id;
            private int count;

            @Override
            public synchronized String makeObject(String s) throws Exception {
                count++;
                return VALUE_NAME + ++id;
            }

            @Override
            public synchronized void destroyObject(String s, String s1) throws Exception {
                count--;
            }

            @Override
            public boolean validateObject(String s, String s1) {
                return true;
            }

            @Override
            public void activateObject(String s, String s1) throws Exception {
            }

            @Override
            public void passivateObject(String s, String s1) throws Exception {
            }
        };
        final GenericKeyedObjectPool<String, String> apachePool = new GenericKeyedObjectPool<String, String>(apacheFactory, poolSize, (byte) 0, 0, poolSize, poolSize, poolSize, false, false, 1000 * 60 * 60, 0, 1000 * 60 * 60, false);
        String object = null;

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            try {
                object = apachePool.borrowObject("key");
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                apachePool.returnObject("key", object);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        logger.info("apache common-pool elapse = {}", (System.currentTimeMillis() - startTime));

        try {
            apachePool.close();
        } catch (Exception ignore) {
        }


        // grizzly
        final PoolableObjectFactory<String, String> grizzlyFactory = new PoolableObjectFactory<String, String>() {
            private static final String VALUE_NAME = "value";
            private int id;
            private int count;

            @Override
            public synchronized String createObject(String s) throws Exception {
                count++;
                return VALUE_NAME + ++id;
            }

            @Override
            public synchronized void destroyObject(String s, String s1) throws Exception {
                count--;
            }

            @Override
            public boolean validateObject(String s, String s1) {
                return true;
            }
        };
        final BaseObjectPool.Builder<String, String> builder = new BaseObjectPool.Builder<String, String>(grizzlyFactory);
        builder.disposable(false);
        builder.keepAliveTimeoutInSecs(-1);
        builder.borrowValidation(false);
        builder.returnValidation(false);
        builder.max(poolSize);
        builder.min(poolSize);
        final ObjectPool<String, String> grizzlyPool = builder.build();

        startTime = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            try {
                object = grizzlyPool.borrowObject("key", 100);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                grizzlyPool.returnObject("key", object);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        logger.info("grizzly pool elapse = {}", (System.currentTimeMillis() - startTime));

        grizzlyPool.destroy();
    }

    @Test
    public void testBenchmarkingInMultiThreads() {
        final int threadCount = 1000;
        final int poolSize = 150;
        final KeyedPoolableObjectFactory<String, String> apacheFactory = new KeyedPoolableObjectFactory<String, String>() {
            private static final String VALUE_NAME = "value";
            private int id;
            private int count;

            @Override
            public synchronized String makeObject(String s) throws Exception {
                count++;
                return VALUE_NAME + ++id;
            }

            @Override
            public synchronized void destroyObject(String s, String s1) throws Exception {
                count--;
            }

            @Override
            public boolean validateObject(String s, String s1) {
                return true;
            }

            @Override
            public void activateObject(String s, String s1) throws Exception {
            }

            @Override
            public void passivateObject(String s, String s1) throws Exception {
            }
        };
        final GenericKeyedObjectPool<String, String> apachePool = new GenericKeyedObjectPool<String, String>(apacheFactory, poolSize, GenericKeyedObjectPool.WHEN_EXHAUSTED_FAIL, 0, poolSize, poolSize, poolSize, false, false, 1000 * 60 * 60, 0, 1000 * 60 * 60, false);

        final ConcurrentLinkedQueue<String> borrowObjects = new ConcurrentLinkedQueue<String>();
        final CountDownLatch startFlag = new CountDownLatch(1);
        final CountDownLatch finishFlag = new CountDownLatch(threadCount * 2);
        final AtomicInteger exceptionCnt = new AtomicInteger();
        final AtomicInteger successCnt = new AtomicInteger();
        for (int i = 0; i < threadCount; i++) {
            final Thread borrowThread = new Thread() {
                @Override
                public void run() {
                    try {
                        startFlag.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    for (int j = 0; j < 30; j++) {
                        try {
                            final String object = apachePool.borrowObject("key");
                            Assert.assertNotNull(object);
                            successCnt.incrementAndGet();
                            Assert.assertTrue(borrowObjects.offer(object));
                        } catch (Exception ignore) {
                            exceptionCnt.incrementAndGet();
                        }
                    }
                    finishFlag.countDown();
                }
            };
            borrowThread.start();

            final Thread returnThread = new Thread() {
                @Override
                public void run() {
                    try {
                        startFlag.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    for (int j = 0; j < 30; j++) {
                        try {
                            final String object = borrowObjects.poll();
                            if (object != null) {
                                apachePool.returnObject("key", object);
                            }
                        } catch (Exception e) {
                            Assert.fail(e.getMessage());
                        }
                    }
                    finishFlag.countDown();
                }
            };
            returnThread.start();
        }
        long startTime = System.currentTimeMillis();
        startFlag.countDown();
        try {
            finishFlag.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("apache common-pool elapse = {}", (System.currentTimeMillis() - startTime));
        try {
            logger.info("apache common-pool max gen-id = {}", apacheFactory.makeObject("key"));
        } catch (Exception ignore) {
        }
        logger.info("apache common-pool success counts = {}", successCnt.get());
        logger.info("apache common-pool exception counts = {}", exceptionCnt.get());

        try {
            apachePool.close();
        } catch (Exception ignore) {
        }


        // grizzly
        final PoolableObjectFactory<String, String> grizzlyFactory = new PoolableObjectFactory<String, String>() {
            private static final String VALUE_NAME = "value";
            private int id;
            private int count;

            @Override
            public synchronized String createObject(String s) throws Exception {
                count++;
                return VALUE_NAME + ++id;
            }

            @Override
            public synchronized void destroyObject(String s, String s1) throws Exception {
                count--;
            }

            @Override
            public boolean validateObject(String s, String s1) {
                return true;
            }
        };
        final BaseObjectPool.Builder<String, String> builder = new BaseObjectPool.Builder<String, String>(grizzlyFactory);
        builder.disposable(false);
        builder.keepAliveTimeoutInSecs(-1);
        builder.borrowValidation(false);
        builder.returnValidation(false);
        builder.max(poolSize);
        builder.min(poolSize);
        final ObjectPool<String, String> grizzlyPool = builder.build();

        final ConcurrentLinkedQueue<String> borrowObjects2 = new ConcurrentLinkedQueue<String>();
        final CountDownLatch startFlag2 = new CountDownLatch(1);
        final CountDownLatch finishFlag2 = new CountDownLatch(threadCount * 2);
        final AtomicInteger exceptionCnt2 = new AtomicInteger();
        final AtomicInteger successCnt2 = new AtomicInteger();
        for (int i = 0; i < threadCount; i++) {
            final Thread borrowThread = new Thread() {
                @Override
                public void run() {
                    try {
                        startFlag2.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    for (int j = 0; j < 30; j++) {
                        try {
                            final String object = grizzlyPool.borrowObject("key", 0);
                            Assert.assertNotNull(object);
                            successCnt2.incrementAndGet();
                            Assert.assertTrue(borrowObjects2.offer(object));
                        } catch (Exception ignore) {
                            exceptionCnt2.incrementAndGet();
                        }
                    }
                    finishFlag2.countDown();
                }
            };
            borrowThread.start();

            final Thread returnThread = new Thread() {
                @Override
                public void run() {
                    try {
                        startFlag2.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    for (int j = 0; j < 30; j++) {
                        try {
                            final String object = borrowObjects2.poll();
                            if (object != null) {
                                grizzlyPool.returnObject("key", object);
                            }
                        } catch (Exception e) {
                            Assert.fail(e.getMessage());
                        }
                    }
                    finishFlag2.countDown();
                }
            };
            returnThread.start();
        }
        startTime = System.currentTimeMillis();
        startFlag2.countDown();
        try {
            finishFlag2.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("grizzly pool elapse = {}", (System.currentTimeMillis() - startTime));
        try {
            logger.info("grizzly pool max gen-id = {}", grizzlyFactory.createObject("key"));
        } catch (Exception ignore) {
        }
        logger.info("grizzly pool success counts = {}", successCnt2.get());
        logger.info("grizzly pool exception counts= {}", exceptionCnt2.get());

        grizzlyPool.destroy();
    }
}
