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

import org.glassfish.grizzly.monitoring.MonitoringAware;

import java.util.concurrent.TimeoutException;

/**
 * Keyed object pooling interface
 *
 * ObjectPool interface mainly defines {@link #borrowObject borrowObject}, {@link #returnObject returnObject} and {@link #removeObject removeObject}.
 *
 * Example of use:
 * {@code
 *     Object obj = null;
 *     try {
 *         obj = pool.borrowObject(key, timeout);
 *         try {
 *         //...use the object...
 *         } catch(Exception e) {
 *             // invalidate the object
 *             try {
 *                 pool.removeObject(key, obj);
 *             } catch(Exception ignore) {
 *             }
 *             // do not return the object to the pool twice
 *             obj = null;
 *         } finally {
 *             // make sure the object is returned to the pool
 *             if(obj != null) {
 *                 try {
 *                     pool.returnObject(key, obj);
 *                 } catch(Exception ignore) {
 *                 }
 *             }
 *         }
 *     } catch(Exception e) {
 *         // failed to borrow an object(pool exhausted, no valid object, interrupted or etc...)
 *     }
 * }
 * @author Bongjae Chang
 */
public interface ObjectPool<K, V> extends MonitoringAware<ObjectPoolProbe> {

    /**
     * Create objects using the {@link PoolableObjectFactory factory} until pool's minimum size, and then place them in the idle object pool
     * <p>
     * {@code createAllMinObjects} is useful for "pre-loading" a pool with idle objects.
     *
     * @param key the key new instances should be added to
     * @throws Exception if an unexpected exception occurred
     */
    public void createAllMinObjects(final K key) throws Exception;

    /**
     * Obtains an instance from this pool
     * <p>
     * Instances returned from this method will have been either newly created with
     * {@link PoolableObjectFactory#createObject createObject} or will be a previously idle object and
     * then validated with {@link PoolableObjectFactory#validateObject validateObject}.
     * <p>
     * By contract, clients should return the borrowed instance using
     * {@link #returnObject returnObject}, {@link #removeObject removeObject}
     * <p>
     * When the pool has been exhausted, a {@link PoolExhaustedException} will be thrown.
     *
     * @param key             the key used to obtain the object
     * @param timeoutInMillis the max time(milli-second) for borrowing the object. If the pool cannot return an instance in given time,
     *                        {@link PoolExhaustedException} will be thrown.
     * @return an instance from this pool
     * @throws PoolExhaustedException when the pool is exhausted
     * @throws NoValidObjectException when the pool cannot or will not return another instance
     * @throws TimeoutException when the pool cannot or will not return another instance because of connection timeout
     * @throws InterruptedException   when the pool is interrupted
     */
    public V borrowObject(final K key, final long timeoutInMillis) throws PoolExhaustedException, NoValidObjectException,
            TimeoutException, InterruptedException;

    /**
     * Return an instance to the pool
     * <p>
     * By contract, {@code value} should have been obtained
     * using {@link #borrowObject borrowObject} using a {@code key} that is equivalent to the one used to
     * borrow the instance in the first place.
     *
     * @param key   the key used to obtain the object
     * @param value a {@link #borrowObject borrowed} instance to be returned
     * @throws Exception if an unexpected exception occurred
     */
    public void returnObject(final K key, final V value) throws Exception;

    /**
     * Removes(invalidates) an object from the pool
     * <p>
     * By contract, {@code value} should have been obtained
     * using {@link #borrowObject borrowObject} using a {@code key} that is equivalent to the one used to
     * borrow the instance in the first place.
     * <p>
     * This method should be used when an object that has been borrowed
     * is determined (due to an exception or other problem) to be invalid.
     *
     * @param key   the key used to obtain the object
     * @param value a {@link #borrowObject borrowed} instance to be removed
     * @throws Exception if an unexpected exception occurred
     */
    public void removeObject(final K key, final V value) throws Exception;

    /**
     * Clears the specified pool, removing all pooled instances corresponding to the given {@code key}
     *
     * @param key the key to clear
     * @throws Exception if an unexpected exception occurred
     */
    public void removeAllObjects(final K key) throws Exception;

    /**
     * Destroy the specified pool, removing all pooled instances, mapping key and statistics corresponding to the given {@code key}
     * <p>
     * After destroying, {@link #borrowObject} with the given {@code key} will be failed.
     *
     * @param key the key to destroy
     * @throws Exception if an unexpected exception occurred
     */
    public void destroy(final K key) throws Exception;

    /**
     * Destroy this pool, and free any resources associated with it
     * <p>
     * Calling other methods such as {@link #createAllMinObjects createAllMinObjects} or {@link #borrowObject borrowObject},
     * {@link #returnObject returnObject} or {@link #removeObject removeObject} or {@link #removeAllObjects removeAllObjects} after invoking
     * this method on a pool will cause them to throw an {@link IllegalStateException}.
     * </p>
     */
    public void destroy();

    /**
     * Returns the total number of instances
     *
     * @param key the key to query
     * @return the total number of instances corresponding to the given {@code key} currently idle and active in this pool or a negative value if unsupported
     */
    public int getPoolSize(final K key);

    /**
     * Returns the total peak number of instances
     *
     * @param key the key to query
     * @return the peak number of instances corresponding to the given {@code key} or a negative value if unsupported
     */
    public int getPeakCount(final K key);

    /**
     * Returns the number of instances currently borrowed from but not yet returned to the pool
     *
     * @param key the key to query
     * @return the number of instances corresponding to the given {@code key} currently borrowed in this pool or a negative value if unsupported
     */
    public int getActiveCount(final K key);

    /**
     * Returns the number of instances currently idle in this pool
     *
     * @param key the key to query
     * @return the number of instances corresponding to the given {@code key} currently idle in this pool or a negative value if unsupported
     */
    public int getIdleCount(final K key);

    /**
     * Returns the total number of instances for all keys
     *
     * @return the total number of instances for all keys managed by this object pool or a negative value if unsupported
     */
    public int getTotalPoolSize();

    /**
     * Returns the highest peak number of instances among all keys
     *
     * @return the highest peak number of instances among all keys managed by this object pool or a negative value if unsupported
     */
    public int getHighestPeakCount();

    /**
     * Returns the total number of instances currently borrowed from but not yet returned to the pool for all keys
     *
     * @return the total number of instances currently borrowed from but not yet returned to the pool for all keys or a negative value if unsupported
     */
    public int getTotalActiveCount();

    /**
     * Returns the total number of instances currently idle in this pool for all keys
     *
     * @return the total number of instances currently idle in this pool for all keys or a negative value if unsupported
     */
    public int getTotalIdleCount();
}
