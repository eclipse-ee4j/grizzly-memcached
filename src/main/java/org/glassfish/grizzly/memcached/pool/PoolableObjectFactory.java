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

/**
 * An interface defining life-cycle methods for instances to be served by a {@link ObjectPool}
 * <p>
 * By contract, when an {@link ObjectPool} delegates to a {@link PoolableObjectFactory},
 * {@link #createObject createObject} is called whenever a new instance is needed.
 * {@link #validateObject validateObject} is invoked for making sure
 * they can be {@link ObjectPool#borrowObject borrowed} or {@link ObjectPool#returnObject returned} from the pool.
 * {@link #destroyObject destroyObject} is invoked on every instance when it is being "dropped" from the pool.
 *
 * @author Bongjae Chang
 */
public interface PoolableObjectFactory<K, V> {

    /**
     * Create an instance that can be served by the pool
     *
     * @param key the key used when constructing the object
     * @return an instance that can be served by the pool
     * @throws Exception if there is a problem creating a new instance
     */
    public V createObject(final K key) throws Exception;

    /**
     * Destroy an instance no longer needed by the pool
     *
     * @param key   the key used when selecting the instance
     * @param value the instance to be destroyed
     * @throws Exception if there is a problem destroying {@code value}
     */
    public void destroyObject(final K key, final V value) throws Exception;

    /**
     * Ensures that the instance is safe to be borrowed and returned by the pool
     *
     * @param key   the key used when selecting the object
     * @param value the instance to be validated
     * @return false if {@code value} is not valid and should be dropped from the pool,
     *         true otherwise
     * @throws Exception if there is a problem validating {@code value}.
     *                   an exception should be avoided as it may be swallowed by the pool implementation.
     */
    public boolean validateObject(final K key, final V value) throws Exception;
}
