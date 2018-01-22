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
 * Defines response's status of the memcached's binary protocol
 * <p>
 * See http://code.google.com/p/memcached/wiki/BinaryProtocolRevamped#Response_Status
 *
 * @author Bongjae Chang
 */
public enum ResponseStatus {
    No_Error(0x0000, "No error"),
    Key_Not_Found(0x0001, "Key not found"),
    Key_Exists(0x0002, "Key exists"),
    Value_Too_Large(0x0003, "Value too large"),
    Invalid_Arguments(0x0004, "Invalid arguments"),
    Item_Not_Stored(0x0005, "Item not stored"),
    Incr_Decr_On_NonNumeric_Value(0x0006, "Incr/Decr on non-numeric value"),
    VBucket_Belongs_To_Another_Server(0x0007, "The vbucket belongs to another server"),
    Authentication_Error(0x0008, "Authentication error"),
    Authentication_Continue(0x0009, "Authentication continue"),
    Authentication_Required(0x0020, "Authentication required or not successful"), // ??
    Further_Authentication_Required(0x0021, "Further authentication steps required"), // ??
    Unknown_Command(0x0081, "Unknown command"),
    Out_Of_Memory(0x0082, "Out of memory"),
    Not_Supported(0x0083, "Not supported"),
    Internal_Error(0x0084, "Internal error"),
    Busy(0x0085, "Busy"),
    Temporary_Failure(0x0086, "Temporary failure");

    private final short status;
    private final String message;

    private ResponseStatus(int status, String message) {
        this.status = (short) (status & 0xffff);
        this.message = message;
    }

    public short status() {
        return status;
    }

    public String message() {
        return message;
    }

    public static ResponseStatus getResponseStatus(final short status) {
        switch (status) {
            case 0x0000:
                return No_Error;
            case 0x0001:
                return Key_Not_Found;
            case 0x0002:
                return Key_Exists;
            case 0x0003:
                return Value_Too_Large;
            case 0x0004:
                return Invalid_Arguments;
            case 0x0005:
                return Item_Not_Stored;
            case 0x0006:
                return Incr_Decr_On_NonNumeric_Value;
            case 0x0007:
                return VBucket_Belongs_To_Another_Server;
            case 0x0008:
                return Authentication_Error;
            case 0x0009:
                return Authentication_Continue;
            case 0x0020:
                return Authentication_Required;
            case 0x0021:
                return Further_Authentication_Required;
            case 0x0081:
                return Unknown_Command;
            case 0x0082:
                return Out_Of_Memory;
            case 0x0083:
                return Not_Supported;
            case 0x0084:
                return Internal_Error;
            case 0x0085:
                return Busy;
            case 0x0086:
                return Temporary_Failure;
            default:
                throw new IllegalArgumentException("invalid status");
        }
    }
}
