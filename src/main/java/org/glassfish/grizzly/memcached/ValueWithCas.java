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

/**
 * The result of memcached's "gets" commands
 * <p>
 * This has value and cas.
 *
 * @author Bongjae Chang
 */
public final class ValueWithCas<V> {

    private final V value;
    private final long cas;

    public ValueWithCas(final V value, final long cas) {
        this.value = value;
        this.cas = cas;
    }

    public V getValue() {
        return value;
    }

    public long getCas() {
        return cas;
    }

    @Override
    public String toString() {
        return "ValueWithCas{" +
                "value=" + value +
                ", cas=" + cas +
                '}';
    }
}
