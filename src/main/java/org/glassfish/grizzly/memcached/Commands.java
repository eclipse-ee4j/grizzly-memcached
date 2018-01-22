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

import java.net.SocketAddress;
import java.util.Map;

/**
 * Defines basic commands of the memcached
 *
 * See http://code.google.com/p/memcached/wiki/NewCommands and http://code.google.com/p/memcached/wiki/BinaryProtocolRevamped
 * And the {@code noReply} parameter means memcached's quiet command such as GetQ, SetQ and etc...
 *
 * @author Bongjae Chang
 */
public interface Commands<K, V> {
    // storage commands

    public boolean set(final K key, final V value, final int expirationInSecs, final boolean noReply);

    public boolean add(final K key, final V value, final int expirationInSecs, final boolean noReply);

    public boolean replace(final K key, final V value, final int expirationInSecs, final boolean noReply);

    public boolean append(final K key, final V value, final boolean noReply);

    public boolean prepend(final K key, final V value, final boolean noReply);

    public boolean cas(final K key, final V value, final int expirationInSecs, final long cas, final boolean noReplys);


    // retrieval commands

    public V get(final K key, final boolean noReply);

    public ValueWithKey<K, V> getKey(final K key, final boolean noReply);

    public ValueWithCas<V> gets(final K key, final boolean noReply);

    public V gat(final K key, final int expirationInSecs, final boolean noReplys);

    public boolean delete(final K key, final boolean noReply);

    public long incr(final K key, final long delta, final long initial, final int expirationInSecs, final boolean noReply);

    public long decr(final K key, final long delta, final long initial, final int expirationInSecs, final boolean noReply);


    // security

    public String saslAuth(final SocketAddress address, final String mechanism, final byte[] data);

    public String saslStep(final SocketAddress address, final String mechanism, final byte[] data);

    public String saslList(final SocketAddress address);


    // statistics

    public Map<String, String> stats(final SocketAddress address);

    public Map<String, String> statsItems(final SocketAddress address, final String item);


    // etc...

    public boolean quit(final SocketAddress address, final boolean noReply);

    public boolean flushAll(final SocketAddress address, final int expirationInSecs, final boolean noReply);

    public boolean touch(final K key, final int expirationInSecs);

    public boolean noop(final SocketAddress addresss);

    public boolean verbosity(final SocketAddress address, final int verbosity);

    public String version(final SocketAddress address);
}
