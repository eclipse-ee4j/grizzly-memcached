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

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * @author Bongjae Chang
 */
public class ConsistentHashStoreTest {

    @Test
    public void testBasicConsistentHash() {
        final ConsistentHashStore<String> consistentHash = new ConsistentHashStore<String>();
        consistentHash.add("server1");
        consistentHash.add("server2");
        consistentHash.add("server3");

        final String selectedServer = consistentHash.get("key");
        Assert.assertNotNull(selectedServer);
        Assert.assertEquals(selectedServer, consistentHash.get("key"));

        consistentHash.remove(selectedServer);
        Assert.assertTrue(!selectedServer.equals(consistentHash.get("key")));

        consistentHash.add(selectedServer);
        Assert.assertEquals(selectedServer, consistentHash.get("key"));
    }

    @Test
    public void testSeveralServersAndKeys() {
        final int initialServerNum = 50;
        final int initialKeyNum = 200;
        final int newServersNum = 10;

        final ConsistentHashStore<String> consistentHash = new ConsistentHashStore<String>();
        final HashMap<String, Set<String>> map = new HashMap<String, Set<String>>();
        for (int i = 0; i < initialServerNum; i++) {
            final String serverName = "server" + i;
            consistentHash.add(serverName);
            map.put(serverName, new HashSet<String>());
        }

        for (int i = 0; i < initialKeyNum; i++) {
            final String key = "key" + i;
            final String selectedServer = consistentHash.get(key);
            Set<String> keySet = map.get(selectedServer);
            keySet.add(key);
        }

        // server failure
        Random random = new Random();
        int serverIndex = random.nextInt(initialServerNum);
        final String failureServer = "server" + serverIndex;
        consistentHash.remove(failureServer);

        // when a server failed, original keys should not have failure server
        for (String failureKey : map.remove(failureServer)) {
            Assert.assertTrue(!failureServer.equals(consistentHash.get(failureKey)));
        }

        // when a server failed, original keys should have original servers.
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            for (String key : entry.getValue()) {
                Assert.assertEquals(entry.getKey(), consistentHash.get(key));
            }
        }

        // when new servers added, some original keys should be distributed into new servers
        for (int i = initialServerNum; i < initialServerNum + newServersNum; i++) {
            consistentHash.add("server" + i);
        }
        boolean distributed = false;
        for (int i = 0; i < initialKeyNum; i++) {
            final String key = "key" + i;
            for (int j = initialServerNum; j < initialServerNum + newServersNum; j++) {
                if (("server" + j).equals(consistentHash.get(key))) {
                    distributed = true;
                    break;
                }
            }
            if (distributed) {
                break;
            }
        }
        Assert.assertTrue(distributed);
    }
}
