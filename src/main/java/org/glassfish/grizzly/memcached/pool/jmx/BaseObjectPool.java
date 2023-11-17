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

package org.glassfish.grizzly.memcached.pool.jmx;

import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;
import org.glassfish.grizzly.monitoring.jmx.JmxObject;

import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.GmbalMBean;
import org.glassfish.gmbal.ManagedObject;

/**
 * JMX managed object for object pool implementations.
 */
@ManagedObject
@Description("Basic object pool with a pool of objects (generally related to network connections) for each key.")
public class BaseObjectPool extends JmxObject {

    private final org.glassfish.grizzly.memcached.pool.BaseObjectPool pool;

    public BaseObjectPool(final org.glassfish.grizzly.memcached.pool.BaseObjectPool pool) {
        this.pool = pool;
    }

    @Override
    public String getJmxName() {
        return pool.getName();
    }

    @Override
    protected void onRegister(GrizzlyJmxManager mom, GmbalMBean bean) {
    }

    @Override
    protected void onDeregister(GrizzlyJmxManager mom) {
    }

    /**
     * Returns the Java type of the managed object pool
     *
     * @return the Java type of the managed object pool.
     */
    @ManagedAttribute(id = "object-pool-type")
    @Description("The Java type of the object pool implementation being used.")
    public String getPoolType() {
        return pool.getClass().getName();
    }

    /**
     * Returns the total number of instances for all keys
     *
     * @return the total number of instances for all keys managed by this object pool or a negative value if unsupported
     */
    @ManagedAttribute(id = "object-pool-total-pool-size")
    @Description("The total number of instances for all keys managed by this object pool.")
    public int getTotalPoolSize() {
        return pool.getTotalPoolSize();
    }

    /**
     * Returns the highest peak number of instances among all keys
     *
     * @return the highest peak number of instances among all keys managed by this object pool or a negative value if unsupported
     */
    @ManagedAttribute(id = "object-pool-highest-peak-count")
    @Description("The highest peak number of instances among all keys managed by this object pool.")
    public int getHighestPeakCount() {
        return pool.getHighestPeakCount();
    }

    /**
     * Returns the total number of instances currently borrowed from but not yet returned to the pool for all keys
     *
     * @return the total number of instances currently borrowed from but not yet returned to the pool for all keys or a negative value if unsupported
     */
    @ManagedAttribute(id = "object-pool-total-active-count")
    @Description(
            "The total number of instances currently borrowed from but not yet returned to the pool for all keys managed by this object pool.")
    public int getTotalActiveCount() {
        return pool.getTotalActiveCount();
    }

    /**
     * Returns the total number of instances currently idle in this pool for all keys
     *
     * @return the total number of instances currently idle in this pool for all keys or a negative value if unsupported
     */
    @ManagedAttribute(id = "object-pool-total-idle-count")
    @Description("The total number of instances currently idle for all keys managed by this object pool.")
    public int getTotalIdleCount() {
        return pool.getTotalIdleCount();
    }

    /**
     * @return the minimum size of this object pool per key.
     */
    @ManagedAttribute(id = "object-pool-min-pool-size-per-key")
    @Description("The initial/minimum number of objects per key managed by this object pool.")
    public int getMinPerKey() {
        return pool.getMin();
    }

    /**
     * @return the maximum size of this object pool per key.
     */
    @ManagedAttribute(id = "object-pool-max-pool-size-per-key")
    @Description("The maximum number of objects per key allowed by this object pool.")
    public int getMaxPerKey() {
        return pool.getMax();
    }

    @ManagedAttribute(id = "object-pool-borrow-validation")
    public boolean isBorrowValidation() {
        return pool.isBorrowValidation();
    }

    @ManagedAttribute(id = "object-pool-return-validation")
    public boolean isReturnValidation() {
        return pool.isReturnValidation();
    }

    @ManagedAttribute(id = "object-pool-disposable")
    public boolean isDisposable() {
        return pool.isDisposable();
    }

    @ManagedAttribute(id = "object-pool-keep-alive-timeout-seconds")
    public long getKeepAliveTimeoutInSecs() {
        return pool.getKeepAliveTimeoutInSecs();
    }

    @ManagedAttribute(id = "object-pool-destroyed")
    public boolean isDestroyed() {
        return pool.isDestroyed();
    }

    @ManagedAttribute(id = "object-pool-keys")
    public String getKeys() {
        return pool.getKeys();
    }
}
