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

package org.glassfish.grizzly.memcached;

import org.glassfish.grizzly.monitoring.MonitoringAware;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The memcached's cache interface
 * <p>
 * This interface extends {@link Commands} and {@link Cache} and has methods related to operation timeout.
 * Additionally, this supports several bulk operations such as {@link #getMulti} and {@link #setMulti} for dramatic performance improvement.
 * <p>
 * By {@link #addServer} and {@link #removeServer}, servers can be added and removed dynamically in this cache.
 * In other words, the managed server list can be changed in runtime by APIs.
 *
 * @author Bongjae Chang
 */
public interface MemcachedCache<K, V> extends Commands<K, V>, Cache<K, V>, MonitoringAware<MemcachedCacheProbe> {
    // extends basic memcached commands

    public boolean set(final K key, final V value, final int expirationInSecs, final boolean noReply, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public Map<K, Boolean> setMulti(final Map<K, V> map, final int expirationInSecs);

    public Map<K, Boolean> setMulti(final Map<K, V> map, final int expirationInSecs, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public boolean add(final K key, final V value, final int expirationInSecs, final boolean noReply, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public boolean replace(final K key, final V value, final int expirationInSecs, final boolean noReply, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public boolean append(final K key, final V value, final boolean noReply, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public boolean prepend(final K key, final V value, final boolean noReply, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public boolean cas(final K key, final V value, final int expirationInSecs, final long cas, final boolean noReply, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public Map<K, Boolean> casMulti(final Map<K, ValueWithCas<V>> map, final int expirationInSecs);

    public Map<K, Boolean> casMulti(final Map<K, ValueWithCas<V>> map, final int expirationInSecs, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public V get(final K key, final boolean noReply, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public Map<K, V> getMulti(final Set<K> keys);

    public Map<K, V> getMulti(final Set<K> keys, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public ValueWithKey<K, V> getKey(final K key, final boolean noReply, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public ValueWithCas<V> gets(final K key, final boolean noReply, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public Map<K, ValueWithCas<V>> getsMulti(final Set<K> keys);

    public Map<K, ValueWithCas<V>> getsMulti(final Set<K> keys, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public V gat(final K key, final int expirationInSecs, final boolean noReply, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    /**
     * delete the item with the given key in memcached
     *
     * @param key item's key
     * @param noReply whether you need to receive a reply or not. true means the quiet operation(no reply).
     * @param writeTimeoutInMillis write timeout
     * @param responseTimeoutInMillis response timeout
     * @return true if the key and the corresponding item is deleted successfully in memcached. false if the deletion is failed(ex. timeout, io failures, ...).
     * Note) true will be returned when the key doesn't exist in memcached(since v1.3.2)
     */
    public boolean delete(final K key, final boolean noReply, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public Map<K, Boolean> deleteMulti(final Set<K> keys);

    public Map<K, Boolean> deleteMulti(final Set<K> keys, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public long incr(final K key, final long delta, final long initial, final int expirationInSecs, final boolean noReply, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public long decr(final K key, final long delta, final long initial, final int expirationInSecs, final boolean noReply, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public String saslAuth(final SocketAddress address, final String mechanism, final byte[] data, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public String saslStep(final SocketAddress address, final String mechanism, final byte[] data, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public String saslList(final SocketAddress address, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public Map<String, String> stats(final SocketAddress address, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public Map<String, String> statsItems(final SocketAddress address, final String item, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public boolean quit(final SocketAddress address, final boolean noReply, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public boolean flushAll(final SocketAddress address, final int expirationInSecs, final boolean noReply, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public boolean touch(final K key, final int expirationInSecs, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public boolean noop(final SocketAddress address, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public boolean verbosity(final SocketAddress address, final int verbosity, final long writeTimeoutInMillis, final long responseTimeoutInMillis);

    public String version(final SocketAddress address, final long writeTimeoutInMillis, final long responseTimeoutInMillis);


    /**
     * Add a specific server in this cache
     *
     * @param serverAddress a specific server's {@link SocketAddress} to be added
     * @return true if the given {@code serverAddress} is added successfully
     */
    public boolean addServer(final SocketAddress serverAddress);

    /**
     * Remove the given server in this cache
     *
     * @param serverAddress the specific server's {@link SocketAddress} to be removed in this cache
     */
    public void removeServer(final SocketAddress serverAddress);

    /**
     * Check if this cache contains the given server
     *
     * @param serverAddress the specific server's {@link SocketAddress} to be checked
     * @return true if this cache already contains the given {@code serverAddress}
     */
    public boolean isInServerList(final SocketAddress serverAddress);

    /**
     * Get current server list
     *
     * @return current server list or empty list if there are no alive servers
     */
    public List<SocketAddress> getCurrentServerList();
}
