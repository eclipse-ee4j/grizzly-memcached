/*
 * Copyright (c) 2012, 2023 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.monitoring.DefaultMonitoringConfig;
import org.glassfish.grizzly.monitoring.MonitoringConfig;
import org.glassfish.grizzly.monitoring.MonitoringUtils;

import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The basic implementation of {@link ObjectPool} for high performance and scalability
 *
 * Example of use:
 * See org.glassfish.grizzly.memcached.pool.BaseObjectPoolTest's test codes
 *
 * This class should be thread-safe.
 *
 * @author Bongjae Chang
 */
public class BaseObjectPool<K, V> implements ObjectPool<K, V> {

    private static final Logger logger = Grizzly.logger(BaseObjectPool.class);

    // retry counts for borrowing an object if it is not valid
    private static final int MAX_VALIDATION_RETRY_COUNT = 3;

    private final PoolableObjectFactory<K, V> factory;
    private final String name;
    private final int min;
    private final int max;
    private final boolean borrowValidation;
    private final boolean returnValidation;
    private final boolean disposable;
    private final long keepAliveTimeoutInSecs;

    private final ConcurrentMap<K, QueuePool<V>> keyedObjectPool = new ConcurrentHashMap<>();
    private final ConcurrentMap<V, K> managedActiveObjects = new ConcurrentHashMap<>();
    private final AtomicBoolean destroyed = new AtomicBoolean();
    private final ScheduledExecutorService scheduledExecutor;
    private final ScheduledFuture<?> scheduledFuture;

    private final DefaultMonitoringConfig<ObjectPoolProbe> baseObjectPoolMonitoringConfig =
            new DefaultMonitoringConfig<ObjectPoolProbe>(ObjectPoolProbe.class) {

                @Override
                public Object createManagementObject() {
                    return createJmxManagementObject();
                }
            };

    private BaseObjectPool(Builder<K, V> builder) {
        this.factory = builder.factory;
        this.name = builder.name;
        this.min = builder.min;
        this.max = builder.max;
        this.borrowValidation = builder.borrowValidation;
        this.returnValidation = builder.returnValidation;
        this.disposable = builder.disposable;
        this.keepAliveTimeoutInSecs = builder.keepAliveTimeoutInSecs;
        if (keepAliveTimeoutInSecs > 0) {
            this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
            this.scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(new EvictionTask(), keepAliveTimeoutInSecs, keepAliveTimeoutInSecs, TimeUnit.SECONDS);
        } else {
            this.scheduledExecutor = null;
            this.scheduledFuture = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createAllMinObjects(final K key) throws NoValidObjectException, TimeoutException {
        if (destroyed.get()) {
            throw new IllegalStateException("pool has already destroyed");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        QueuePool<V> pool = keyedObjectPool.get(key);
        if (pool == null) {
            final QueuePool<V> newPool = new QueuePool<V>(max);
            final QueuePool<V> oldPool = keyedObjectPool.putIfAbsent(key, newPool);
            pool = oldPool == null ? newPool : oldPool;
        }
        if (pool.destroyed.get()) {
            throw new IllegalStateException("pool already has destroyed. key=" + key);
        }
        for (int i = 0; i < max; i++) {
            V result = createIfUnderSpecificSize(min, pool, key, borrowValidation);
            if (result == null) {
                break;
            }
            if (!pool.queue.offer(result)) {
                try {
                    factory.destroyObject(key, result);
                } catch (Exception ignore) {
                }
                pool.poolSizeHint.decrementAndGet();
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllObjects(final K key) {
        if (destroyed.get()) {
            throw new IllegalStateException("pool has already destroyed");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        final QueuePool<V> pool = keyedObjectPool.get(key);
        if (pool == null) {
            return;
        }
        if (pool.destroyed.get()) {
            return;
        }
        clearPool(pool, key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy(final K key) throws Exception {
        if (destroyed.get()) {
            throw new IllegalStateException("pool has already destroyed");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        final QueuePool<V> pool = keyedObjectPool.remove(key);
        if (pool == null) {
            return;
        }
        if (pool.destroyed.get()) {
            return;
        }
        clearPool(pool, key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V borrowObject(final K key, final long timeoutInMillis)
            throws PoolExhaustedException, NoValidObjectException, TimeoutException, InterruptedException {
        if (destroyed.get()) {
            throw new IllegalStateException("pool already has destroyed");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        QueuePool<V> pool = keyedObjectPool.get(key);
        if (pool == null) {
            final QueuePool<V> newPool = new QueuePool<V>(max);
            final QueuePool<V> oldPool = keyedObjectPool.putIfAbsent(key, newPool);
            pool = oldPool == null ? newPool : oldPool;
        }
        if (pool.destroyed.get()) {
            throw new IllegalStateException("pool already has destroyed. key=" + key);
        }
        V result;
        int retryCount = 0;
        boolean disposableCreation;
        do {
            disposableCreation = false;
            result = createIfUnderSpecificSize(min, pool, key, false);
            if (result == null) {
                result = pool.queue.poll();
            }
            if (result == null) {
                result = createIfUnderSpecificSize(max, pool, key, false);
            }
            if (result == null) {
                if (timeoutInMillis < 0 && !disposable) {
                    result = pool.queue.take();
                } else {
                    result = pool.queue.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
                }
            }
            if (result == null && disposable) {
                try {
                    result = factory.createObject(key);
                } catch (Exception e) {
                    throw new NoValidObjectException(e);
                }
                disposableCreation = true;
            }
            if (result == null) {
                throw new PoolExhaustedException("pool is exhausted");
            }
            if (borrowValidation) {
                boolean valid = false;
                try {
                    valid = factory.validateObject(key, result);
                } catch (Exception ignore) {
                }
                if (valid) {
                    break; // success
                } else {
                    try {
                        factory.destroyObject(key, result);
                    } catch (Exception ignore) {
                    }
                    if (!disposableCreation) {
                        pool.poolSizeHint.decrementAndGet();
                    }
                    result = null;
                    retryCount++; // retry
                }
            } else {
                break; // success
            }
        } while (borrowValidation && retryCount <= MAX_VALIDATION_RETRY_COUNT);
        if (borrowValidation && result == null) {
            throw new NoValidObjectException("there is no valid object");
        }
        if (result != null && pool.destroyed.get()) {
            try {
                factory.destroyObject(key, result);
            } catch (Exception ignore) {
            }
            if (!disposableCreation) {
                pool.poolSizeHint.decrementAndGet();
            }
            throw new IllegalStateException("pool already has destroyed. key=" + key);
        }
        if (result != null && !disposableCreation) {
            managedActiveObjects.put(result, key);
        }
        return result;
    }

    private V createIfUnderSpecificSize(final int specificSize, final QueuePool<V> pool, final K key,
                                        final boolean validation) throws NoValidObjectException, TimeoutException {
        if (destroyed.get()) {
            throw new IllegalStateException("pool has already destroyed");
        }
        if (pool == null) {
            throw new IllegalArgumentException("pool must not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (specificSize >= pool.poolSizeHint.incrementAndGet()) {
            try {
                final V result = factory.createObject(key);
                if (result == null) {
                    pool.poolSizeHint.decrementAndGet();
                    throw new IllegalStateException("failed to create the object. the created object must not be null");
                } else {
                    if (validation) {
                        boolean valid = false;
                        try {
                            valid = factory.validateObject(key, result);
                        } catch (Exception ignore) {
                        }
                        if (!valid) {
                            try {
                                factory.destroyObject(key, result);
                            } catch (Exception ignore) {
                            }
                            pool.poolSizeHint.decrementAndGet();
                            return null;
                        }
                    }
                    final int currentSizeHint = pool.poolSizeHint.get();
                    if (currentSizeHint > pool.peakSizeHint) {
                        pool.peakSizeHint = currentSizeHint;
                    }
                    return result;
                }
            } catch (TimeoutException te) {
                pool.poolSizeHint.decrementAndGet();
                throw te;
            } catch (Exception e) {
                pool.poolSizeHint.decrementAndGet();
                throw new NoValidObjectException(e);
            }
        } else {
            pool.poolSizeHint.decrementAndGet();
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void returnObject(final K key, final V value) {
        if (destroyed.get()) {
            throw new IllegalStateException("pool has already destroyed");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (value == null) {
            return;
        }
        final K managed = managedActiveObjects.remove(value);
        final QueuePool<V> pool = keyedObjectPool.get(key);
        if (pool == null || managed == null) {
            try {
                factory.destroyObject(key, value);
            } catch (Exception ignore) {
            }
            return;
        }
        if (this.returnValidation) {
            boolean valid = false;
            try {
                valid = factory.validateObject(key, value);
            } catch (Exception ignore) {
            }
            if (!valid) {
                try {
                    factory.destroyObject(key, value);
                } catch (Exception ignore) {
                }
                pool.poolSizeHint.decrementAndGet();
                return;
            }
        }

        if (pool.destroyed.get() || !pool.queue.offer(value)) {
            try {
                factory.destroyObject(key, value);
            } catch (Exception ignore) {
            }
            pool.poolSizeHint.decrementAndGet();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeObject(K key, V value) {
        if (destroyed.get()) {
            throw new IllegalStateException("pool has already destroyed");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (value == null) {
            return;
        }
        final K managed = managedActiveObjects.remove(value);
        final QueuePool<V> pool = keyedObjectPool.get(key);
        if (pool == null) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "the pool was not found. managed={0}, key={1}, value={2}", new Object[]{managed, key, value});
            }
            try {
                factory.destroyObject(key, value);
            } catch (Exception ignore) {
            }
            return;
        }

        final boolean removed = pool.queue.remove(value);
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "pool.queue.remove={0}, key={1}, value={2}", new Object[]{removed, key, value});
        }

        try {
            factory.destroyObject(key, value);
        } catch (Exception ignore) {
        }
        if (managed != null || removed) {
            pool.poolSizeHint.decrementAndGet();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
        }

        for (Map.Entry<K, QueuePool<V>> entry : keyedObjectPool.entrySet()) {
            final K key = entry.getKey();
            final QueuePool<V> pool = entry.getValue();
            pool.destroyed.compareAndSet(false, true);
            clearPool(pool, key);
        }
        keyedObjectPool.clear();
        for (Map.Entry<V, K> entry : managedActiveObjects.entrySet()) {
            final V object = entry.getKey();
            final K key = entry.getValue();
            try {
                factory.destroyObject(key, object);
            } catch (Exception ignore) {
            }
        }
        managedActiveObjects.clear();
    }

    private void clearPool(final QueuePool<V> pool, final K key) {
        if (pool == null || key == null) {
            return;
        }
        V object;
        while ((object = pool.queue.poll()) != null) {
            try {
                factory.destroyObject(key, object);
            } catch (Exception ignore) {
            }
            pool.poolSizeHint.decrementAndGet();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPoolSize(final K key) {
        if (destroyed.get()) {
            return -1;
        }
        if (key == null) {
            return -1;
        }
        final QueuePool<V> pool = keyedObjectPool.get(key);
        if (pool == null) {
            return 0;
        }
        return pool.getPoolSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPeakCount(final K key) {
        if (destroyed.get()) {
            return -1;
        }
        if (key == null) {
            return -1;
        }
        final QueuePool<V> pool = keyedObjectPool.get(key);
        if (pool == null) {
            return 0;
        }
        return pool.getPeakCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getActiveCount(final K key) {
        if (destroyed.get()) {
            return -1;
        }
        if (key == null) {
            return -1;
        }
        final QueuePool<V> pool = keyedObjectPool.get(key);
        if (pool == null) {
            return 0;
        }
        return pool.getActiveCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIdleCount(final K key) {
        if (destroyed.get()) {
            return -1;
        }
        if (key == null) {
            return -1;
        }
        final QueuePool<V> pool = keyedObjectPool.get(key);
        if (pool == null) {
            return 0;
        }
        return pool.getIdleCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTotalPoolSize() {
        if (destroyed.get()) {
            return -1;
        }
        return keyedObjectPool.values().stream().mapToInt(QueuePool::getPoolSize).filter(i -> i > 0).sum();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHighestPeakCount() {
        if (destroyed.get()) {
            return -1;
        }
        final OptionalInt maxCount =
                keyedObjectPool.values().stream().mapToInt(QueuePool::getPeakCount).filter(i -> i > 0).max();
        return maxCount.orElse(-1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTotalActiveCount() {
        if (destroyed.get()) {
            return -1;
        }
        return keyedObjectPool.values().stream().mapToInt(QueuePool::getActiveCount).filter(i -> i > 0).sum();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTotalIdleCount() {
        if (destroyed.get()) {
            return -1;
        }
        return keyedObjectPool.values().stream().mapToInt(QueuePool::getIdleCount).filter(i -> i > 0).sum();
    }

    public String getName() {
        return name;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public boolean isBorrowValidation() {
        return borrowValidation;
    }

    public boolean isReturnValidation() {
        return returnValidation;
    }

    public boolean isDisposable() {
        return disposable;
    }

    public long getKeepAliveTimeoutInSecs() {
        return keepAliveTimeoutInSecs;
    }

    public boolean isDestroyed() {
        return destroyed.get();
    }

    public String getKeys() {
        return keyedObjectPool.keySet().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MonitoringConfig<ObjectPoolProbe> getMonitoringConfig() {
        return baseObjectPoolMonitoringConfig;
    }

    /**
     * Create the object pool JMX management object.
     *
     * @return the object pool JMX management object.
     */
    private Object createJmxManagementObject() {
        return MonitoringUtils
                .loadJmxObject("org.glassfish.grizzly.memcached.pool.jmx.BaseObjectPool", this, BaseObjectPool.class);
    }

    /**
     * For storing idle objects, {@link BlockingQueue} will be used.
     * If this pool has max size(bounded pool), it uses {@link LinkedBlockingQueue}.
     * Otherwise, this pool uses unbounded queue like {@link LinkedBlockingQueue} for idle objects.
     */
    private static class QueuePool<V> {
        private final AtomicInteger poolSizeHint = new AtomicInteger();
        private volatile int peakSizeHint = 0;
        private final BlockingQueue<V> queue;
        private final AtomicBoolean destroyed = new AtomicBoolean();

        private QueuePool(final int max) {
            if (max <= 0 || max == Integer.MAX_VALUE) {
                queue = new LinkedTransferQueue<>();

            } else {
                queue = new LinkedBlockingQueue<V>(max);
            }
        }

        private int getPoolSize() {
            return poolSizeHint.get();
        }

        private int getPeakCount() {
            return peakSizeHint;
        }

        private int getActiveCount() {
            return poolSizeHint.get() - queue.size();
        }

        private int getIdleCount() {
            return queue.size();
        }
    }

    /**
     * This task which is scheduled by pool's KeepAliveTime will evict idle objects until pool'size will reach the min's.
     */
    private class EvictionTask implements Runnable {

        private final AtomicBoolean running = new AtomicBoolean();

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            if (!running.compareAndSet(false, true)) {
                return;
            }
            try {
                for (Map.Entry<K, QueuePool<V>> entry : keyedObjectPool.entrySet()) {
                    final K key = entry.getKey();
                    final QueuePool<V> pool = entry.getValue();
                    if (pool.destroyed.get()) {
                        continue;
                    }
                    while (!pool.destroyed.get() && min < pool.poolSizeHint.get()) {
                        final V object = pool.queue.poll();
                        if (object == null) {
                            break;
                        }
                        try {
                            factory.destroyObject(key, object);
                        } catch (Exception ignore) {
                        }
                        pool.poolSizeHint.decrementAndGet();
                    }
                }
            } finally {
                running.set(false);
            }
        }
    }

    public static class Builder<K, V> {
        private static final int DEFAULT_MIN = 5;
        private static final int DEFAULT_MAX = Integer.MAX_VALUE;
        private static final boolean DEFAULT_BORROW_VALIDATION = false;
        private static final boolean DEFAULT_RETURN_VALIDATION = false;
        private static final boolean DEFAULT_DISPOSABLE = false;
        private static final long DEFAULT_KEEP_ALIVE_TIMEOUT_IN_SEC = 30 * 60; // 30min
        private final PoolableObjectFactory<K, V> factory;
        private String name = "";
        private int min = DEFAULT_MIN;
        private int max = DEFAULT_MAX;
        private boolean borrowValidation = DEFAULT_BORROW_VALIDATION;
        private boolean returnValidation = DEFAULT_RETURN_VALIDATION;
        private boolean disposable = DEFAULT_DISPOSABLE;
        private long keepAliveTimeoutInSecs = DEFAULT_KEEP_ALIVE_TIMEOUT_IN_SEC;

        /**
         * BaseObjectPool's builder constructor
         *
         * @param factory {@link PoolableObjectFactory} which is for creating, validating and destroying an object
         */
        public Builder(PoolableObjectFactory<K, V> factory) {
            this.factory = factory;
        }

        public Builder<K, V> name(final String name) {
            if (name != null) {
                this.name = name;
            }
            return this;
        }

        /**
         * Set minimum size of this pool
         * <p>
         * Default is 5.
         *
         * @param min min size
         * @return this builder
         */
        public Builder<K, V> min(final int min) {
            if (min >= 0) {
                this.min = min;
            }
            return this;
        }

        /**
         * Set maximum size of this pool
         * <p>
         * Default is unbounded as {@link Integer#MAX_VALUE}.
         *
         * @param max max size
         * @return this builder
         */
        public Builder<K, V> max(final int max) {
            if (max >= 1) {
                this.max = max;
            }
            return this;
        }

        /**
         * Set whether this pool should validate the object by {@link PoolableObjectFactory#validateObject} before returning a borrowed object to the user
         * <p>
         * Default is false.
         *
         * @param borrowValidation true if validation will be needed
         * @return this builder
         */
        public Builder<K, V> borrowValidation(final boolean borrowValidation) {
            this.borrowValidation = borrowValidation;
            return this;
        }

        /**
         * Set whether this pool should validate the object by {@link PoolableObjectFactory#validateObject} before returning a borrowed object to the pool
         * <p>
         * Default is false.
         *
         * @param returnValidation true if validation will be needed
         * @return this builder
         */
        public Builder<K, V> returnValidation(final boolean returnValidation) {
            this.returnValidation = returnValidation;
            return this;
        }

        /**
         * Set disposable property
         * <p>
         * If this pool is bounded and doesn't have idle objects any more, temporary object will be returned to the user if {@code disposable} is true.
         * The disposable object will be returned to the pool, it will be destroyed silently.
         * Default is false.
         *
         * @param disposable true if the pool allows disposable objects
         * @return this builder
         */
        public Builder<K, V> disposable(final boolean disposable) {
            this.disposable = disposable;
            return this;
        }

        /**
         * Set the KeepAliveTimeout of this pool
         * <p>
         * This pool will schedule EvictionTask with this interval.
         * EvictionTask will evict idle objects if this pool has more than min objects.
         * If the given parameter is negative, this pool never schedules EvictionTask.
         * Default is 1800.
         *
         * @param keepAliveTimeoutInSecs KeepAliveTimeout in seconds
         * @return this builder
         */
        public Builder<K, V> keepAliveTimeoutInSecs(final long keepAliveTimeoutInSecs) {
            this.keepAliveTimeoutInSecs = keepAliveTimeoutInSecs;
            return this;
        }

        /**
         * Create an {@link ObjectPool} instance with this builder's properties
         *
         * @return an object pool
         */
        public ObjectPool<K, V> build() {
            if (min > max) {
                max = min;
            }
            return new BaseObjectPool<K, V>(this);
        }
    }

    @Override
    public String toString() {
        return "BaseObjectPool{" +
                "name=" + name +
                ", keepAliveTimeoutInSecs=" + keepAliveTimeoutInSecs +
                ", disposable=" + disposable +
                ", borrowValidation=" + borrowValidation +
                ", returnValidation=" + returnValidation +
                ", max=" + max +
                ", min=" + min +
                ", factory=" + factory +
                '}';
    }
}
