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

package org.glassfish.grizzly.memcached.zookeeper;

/**
 * Listener interface for barrier in the zookeeper client
 *
 * @author Bongjae Chang
 */
public interface BarrierListener {

    /**
     * Called when the barrier is registered
     *
     * @param regionName  current region name
     * @param path        current data path
     * @param remoteBytes the data of the zookeeper server. This could be null.
     */
    public void onInit(final String regionName, final String path, final byte[] remoteBytes);

    /**
     * Called by zookeeper clients at the same time when all are prepared for commiting something
     *
     * @param regionName  current region name
     * @param path        current data path
     * @param remoteBytes the changed data of the zookeeper server
     */
    public void onCommit(final String regionName, final String path, final byte[] remoteBytes);

    /**
     * Called when the barrier is unregistered
     *
     * @param regionName current region name
     */
    public void onDestroy(final String regionName);
}
