/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.memcached.MemcachedCache;

import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Bongjae Chang
 */
public class PreferRemoteConfigBarrierListener extends CacheServerListBarrierListener {

    private static final Logger LOGGER = Grizzly.logger(PreferRemoteConfigBarrierListener.class);

    public PreferRemoteConfigBarrierListener(final MemcachedCache cache, final Set<SocketAddress> cacheServerSet) {
        super(cache, cacheServerSet);
    }

    @Override
    public void onInit(final String regionName, final String path, final byte[] remoteBytes) {
        if (remoteBytes == null || remoteBytes.length == 0) {
            throw new IllegalStateException("remote config was not ready. path=" + path + ", cacheName=" + cacheName);
        }
        final String remoteCacheServerList;
        try {
            remoteCacheServerList = new String(remoteBytes, DEFAULT_SERVER_LIST_CHARSET);
        } catch (UnsupportedEncodingException uee) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "failed to encode the cache server list from the remote. path=" + path + ", cacheName=" + cacheName, uee);
            }
            throw new IllegalStateException("remote config was not ready. path=" + path + ", cacheName=" + cacheName);
        }
        final Set<SocketAddress> remoteCacheServers = getAddressesFromStringList(remoteCacheServerList);
        if (remoteCacheServerList.isEmpty()) {
            throw new IllegalStateException("remote config was not ready. path=" + path + ", cacheName=" + cacheName);
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "remote config is ready. remoteCacheServers={0}", remoteCacheServers);
        }
        // initializes local server list with remote config
        localCacheServerSet.clear();
        for (final SocketAddress address : remoteCacheServers) {
            localCacheServerSet.add(address);
            cache.addServer(address);
        }
        super.onInit(regionName, path, remoteBytes);
    }
}
