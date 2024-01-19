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

import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.GmbalMBean;
import org.glassfish.gmbal.Impact;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedData;
import org.glassfish.gmbal.ManagedObject;
import org.glassfish.gmbal.ManagedOperation;
import org.glassfish.gmbal.ParameterNames;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;
import org.glassfish.grizzly.memcached.pool.ObjectPool;
import org.glassfish.grizzly.monitoring.jmx.JmxObject;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * JMX managed object for grizzly memcached cache implementations.
 */
@ManagedObject
@Description("Memcached cache client implemented using Grizzly.")
public class GrizzlyMemcachedCache extends JmxObject {

    private final org.glassfish.grizzly.memcached.GrizzlyMemcachedCache cache;

    private GrizzlyJmxManager mom;

    private Transport currentTransport;
    private Object transportJmx;

    private ObjectPool currentConnectionPool;
    private Object connectionPoolJmx;

    public GrizzlyMemcachedCache(final org.glassfish.grizzly.memcached.GrizzlyMemcachedCache cache) {
        this.cache = cache;
    }

    @Override
    public String getJmxName() {
        return cache.getName();
    }

    @Override
    protected void onRegister(GrizzlyJmxManager mom, GmbalMBean gmbalMBean) {
        this.mom = mom;
        rebuildSubTree();
    }

    @Override
    protected void onDeregister(GrizzlyJmxManager mom) {
        this.mom = null;
    }

    /**
     * Returns the Java type of the managed object pool
     *
     * @return the Java type of the managed object pool.
     */
    @ManagedAttribute(id = "grizzly-memcached-cache-type")
    @Description("The Java type of the memcached cache implementation being used.")
    public String getCacheType() {
        return cache.getClass().getName();
    }

    @ManagedAttribute(id = "grizzly-memcached-cache-transport")
    @Description("A string representation of the transport object used by this memcached cache.")
    public String getTransport() {
        return cache.getTransport().toString();
    }

    @ManagedAttribute(id = "grizzly-memcached-cache-connection-timeout-millis")
    public long getConnectTimeoutInMillis() {
        return cache.getConnectTimeoutInMillis();
    }

    @ManagedAttribute(id = "grizzly-memcached-cache-write-timeout-millis")
    public long getWriteTimeoutInMillis() {
        return cache.getWriteTimeoutInMillis();
    }

    @ManagedAttribute(id = "grizzly-memcached-cache-response-timeout-millis")
    public long getResponseTimeoutInMillis() {
        return cache.getResponseTimeoutInMillis();
    }

    @ManagedAttribute(id = "grizzly-memcached-cache-health-monitor-interval-seconds")
    public long getHealthMonitorIntervalInSecs() {
        return cache.getHealthMonitorIntervalInSecs();
    }

    @ManagedAttribute(id = "grizzly-memcached-cache-failover")
    public boolean isFailover() {
        return cache.isFailover();
    }

    @ManagedAttribute(id = "grizzly-memcached-cache-prefer-remote-config")
    public boolean isPreferRemoteConfig() {
        return cache.isPreferRemoteConfig();
    }

    @ManagedAttribute(id = "grizzly-memcached-cache-zookeeper-supported")
    public boolean isZooKeeperSupported() {
        return cache.isZooKeeperSupported();
    }

    @ManagedAttribute(id = "grizzly-memcached-cache-zookeeper-server-list-path")
    public String getZooKeeperServerListPath() {
        return cache.getZooKeeperServerListPath();
    }

    @ManagedAttribute(id = "grizzly-memcached-cache-current-server-list-from-zookeeper")
    public String getCurrentServerListFromZooKeeper() {
        return cache.getCurrentServerListFromZooKeeper();
    }
    @ManagedAttribute(id = "grizzly-memcached-cache-current-server-list")
    public String getCurrentServerList() {
        return cache.getCurrentServerList().toString();
    }

    @ManagedOperation(id = "grizzly-memcached-cache-connection-size", impact = Impact.INFO)
    @Description("The total number of connections currently idle and active in this pool or a negative value if unsupported.")
    @ParameterNames({"hostname", "port"})
    public int getConnectionSize( final String hostname, final int port) {
        return cache.getConnectionSize(getAddress(hostname, port));
    }

    @ManagedOperation(id = "grizzly-memcached-cache-peak-count", impact = Impact.INFO)
    @Description("The peak number of connections or a negative value if unsupported.")
    @ParameterNames({"hostname","port"})
    public int getPeakCount(final String hostname, final int port) {
        return cache.getPeakCount(getAddress(hostname, port));
    }

    @ManagedOperation(id = "grizzly-memcached-cache-active-count", impact = Impact.INFO)
    @Description("The number of connections currently borrowed in this pool or a negative value if unsupported.")
    @ParameterNames({"hostname","port"})
    public int getActiveCount(final String hostname, final int port) {
        return cache.getActiveCount(getAddress(hostname, port));
    }

    @ManagedOperation(id = "grizzly-memcached-cache-idle-count", impact = Impact.INFO)
    @Description("The number of connections currently idle in this pool or a negative value if unsupported.")
    @ParameterNames({"hostname","port"})
    public int getIdleCount(final String hostname, final int port) {
        return cache.getIdleCount(getAddress(hostname, port));
    }

    @ManagedOperation(id = "grizzly-memcached-connection-stat", impact = Impact.INFO)
    @Description("The stat of connections in this pool.")
    @ParameterNames({"hostname","port"})
    public CompositeConnectionStat getConnectionStat(final String hostname, final int port) {
        final SocketAddress server = getAddress(hostname, port);
        return new CompositeConnectionStat(cache.getConnectionSize(server), cache.getPeakCount(server),
                                           cache.getActiveCount(server), cache.getIdleCount(server));
    }

    @ManagedAttribute(id = "grizzly-memcached-cache-jmx-enabled")
    public boolean isJmxEnabled() {
        return cache.isJmxEnabled();
    }

    private void rebuildSubTree() {
        final Transport transport = cache.getTransport();
        if (currentTransport != transport) {
            if (currentTransport != null) {
                mom.deregister(transportJmx);
                currentTransport = null;
                transportJmx = null;
            }
            if (transport != null) {
                final Object jmx = transport.getMonitoringConfig().createManagementObject();
                mom.register(this, jmx);
                currentTransport = transport;
                transportJmx = jmx;
            }
        }

        final ObjectPool connectionPool = cache.getConnectionPool();
        if (currentConnectionPool != connectionPool) {
            if (currentConnectionPool != null) {
                mom.deregister(connectionPoolJmx);
                currentConnectionPool = null;
                connectionPoolJmx = null;
            }
            if (connectionPool != null) {
                final Object jmx = connectionPool.getMonitoringConfig().createManagementObject();
                mom.register(this, jmx);
                currentConnectionPool = connectionPool;
                connectionPoolJmx = jmx;
            }
        }
    }

    private static SocketAddress getAddress(final String hostname, final int port) {
        try {
            return new InetSocketAddress(hostname, port);
        } catch(Exception ignore) {
            return null;
        }
    }

    @ManagedData(name="Connection Stat")
    private static class CompositeConnectionStat {
        @ManagedAttribute(id = "connections")
        @Description("The total number of connections currently idle and active in this pool or a negative value if unsupported.")
        private final int connectionSize;

        @ManagedAttribute(id = "peak")
        @Description("The peak number of connections or a negative value if unsupported.")
        private final int peakCount;

        @ManagedAttribute(id = "active")
        @Description("The number of connections currently borrowed in this pool or a negative value if unsupported.")
        private final int activeCount;

        @ManagedAttribute(id = "idle")
        @Description("The number of connections currently idle in this pool or a negative value if unsupported.")
        private final int idleCount;

        private CompositeConnectionStat(int connectionSize, int peakCount, int activeCount, int idleCount) {
            this.connectionSize = connectionSize;
            this.peakCount = peakCount;
            this.activeCount = activeCount;
            this.idleCount = idleCount;
        }
    }
}
