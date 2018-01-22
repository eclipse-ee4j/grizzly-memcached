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
 * This exception will be thrown when the pool cannot create a new instance by {@link PoolableObjectFactory#createObject}
 * or there are no valid instances which used to be tested by {@link PoolableObjectFactory#validateObject}
 * 
 * @author Bongjae Chang
 */
public class NoValidObjectException extends Exception {

    static final long serialVersionUID = -4079289193871342863L;

    public NoValidObjectException() {
        super();
    }

    public NoValidObjectException(String message) {
        super(message);
    }

    public NoValidObjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoValidObjectException(Throwable cause) {
        super(cause);
    }
}
