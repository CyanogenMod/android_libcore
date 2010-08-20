/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

// BEGIN android-note
// address length was changed from long to int for performance reasons.
// END android-note

package org.apache.harmony.luni.platform;

import java.nio.ByteOrder;

/**
 * The platform address class is an unsafe virtualization of an OS memory block.
 */
public class PlatformAddress implements Comparable {
    public static final int SIZEOF_JBYTE = 1;
    public static final int SIZEOF_JSHORT = 2;
    public static final int SIZEOF_JINT = 4;
    public static final int SIZEOF_JSIZE = 4;
    public static final int SIZEOF_JFLOAT = 4;
    public static final int SIZEOF_JLONG = 8;
    public static final int SIZEOF_JDOUBLE = 8;

    /**
     * This final field defines the sentinel for an unknown address value.
     */
    static final int UNKNOWN = -1;

    /**
     * NULL is the canonical address with address value zero.
     */
    public static final PlatformAddress NULL = new PlatformAddress(0, 0);

    public static final RuntimeMemorySpy memorySpy = new RuntimeMemorySpy();

    final int osaddr;

    final long size;

    PlatformAddress(int address, long size) {
        super();
        osaddr = address;
        this.size = size;
    }

    /**
     * Sending auto free to an address means that, when this subsystem has
     * allocated the memory, it will automatically be freed when this object is
     * collected by the garbage collector if the memory has not already been
     * freed explicitly.
     *
     */
    public final void autoFree() {
        memorySpy.autoFree(this);
    }

    public PlatformAddress duplicate() {
        return PlatformAddressFactory.on(osaddr, size);
    }

    public PlatformAddress offsetBytes(int offset) {
        return PlatformAddressFactory.on(osaddr + offset, size - offset);
    }

    public final void moveTo(PlatformAddress dst, long numBytes) {
        OSMemory.memmove(dst.osaddr, osaddr, numBytes);
    }

    public final boolean equals(Object other) {
        return (other instanceof PlatformAddress)
                && (((PlatformAddress) other).osaddr == osaddr);
    }

    public final int hashCode() {
        return (int) osaddr;
    }

    public void free() {
        // Memory spys can veto the basic free if they determine the memory was
        // not allocated.
        if (memorySpy.free(this)) {
            OSMemory.free(osaddr);
        }
    }

    public final void setByte(int offset, byte value) {
        OSMemory.pokeByte(osaddr + offset, value);
    }

    public final void setByteArray(int offset, byte[] bytes, int bytesOffset, int length) {
        OSMemory.setByteArray(osaddr + offset, bytes, bytesOffset, length);
    }

    public final void setShortArray(int offset, short[] shorts,
            int shortsOffset, int length, boolean swap) {
        OSMemory.setShortArray(osaddr + offset, shorts, shortsOffset, length, swap);
    }

    public final void setIntArray(int offset, int[] ints,
            int intsOffset, int length, boolean swap) {
        OSMemory.setIntArray(osaddr + offset, ints, intsOffset, length, swap);
    }

    public final void setFloatArray(int offset, float[] floats,
            int floatsOffset, int length, boolean swap) {
        OSMemory.setFloatArray(osaddr + offset, floats, floatsOffset, length, swap);
    }

    public final byte peekByte(int offset) {
        return OSMemory.peekByte(osaddr + offset);
    }

    public final void peekByteArray(int offset, byte[] bytes, int bytesOffset, int length) {
        OSMemory.peekByteArray(osaddr + offset, bytes, bytesOffset, length);
    }

    public final void setShort(int offset, short value, ByteOrder order) {
        OSMemory.pokeShort(osaddr + offset, value, order.needsSwap);
    }

    public final short peekShort(int offset, ByteOrder order) {
        return OSMemory.peekShort(osaddr + offset, order.needsSwap);
    }

    public final void setInt(int offset, int value, ByteOrder order) {
        OSMemory.pokeInt(osaddr + offset, value, order.needsSwap);
    }

    public final int peekInt(int offset, ByteOrder order) {
        return OSMemory.peekInt(osaddr + offset, order.needsSwap);
    }

    public final void setLong(int offset, long value, ByteOrder order) {
        OSMemory.pokeLong(osaddr + offset, value, order.needsSwap);
    }

    public final long peekLong(int offset, ByteOrder order) {
        return OSMemory.peekLong(osaddr + offset, order.needsSwap);
    }

    public final void setFloat(int offset, float value, ByteOrder order) {
        OSMemory.pokeFloat(osaddr + offset, value, order.needsSwap);
    }

    public final float peekFloat(int offset, ByteOrder order) {
        return OSMemory.peekFloat(osaddr + offset, order.needsSwap);
    }

    public final void setDouble(int offset, double value, ByteOrder order) {
        OSMemory.pokeDouble(osaddr + offset, value, order.needsSwap);
    }

    public final double peekDouble(int offset, ByteOrder order) {
        return OSMemory.peekDouble(osaddr + offset, order.needsSwap);
    }

    public final int toInt() {
        return osaddr;
    }

    public final String toString() {
        return "PlatformAddress[" + osaddr + "]";
    }

    public final long getSize() {
        return size;
    }

    public final int compareTo(Object other) {
        if (other == null) {
            throw new NullPointerException(); // per spec.
        }
        if (other instanceof PlatformAddress) {
            int otherPA = ((PlatformAddress) other).osaddr;
            if (osaddr == otherPA) {
                return 0;
            }
            return osaddr < otherPA ? -1 : 1;
        }

        throw new ClassCastException(); // per spec.
    }
}
