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

package org.apache.harmony.luni.platform;

import java.nio.ByteOrder;

public class PlatformAddress {
    // TODO: should be long on 64-bit devices; int for performance.
    protected int osaddr;
    protected final long size;

    public PlatformAddress(int address, long size) {
        this.osaddr = address;
        this.size = size;
    }

    public void free() {
    }

    public final void pokeByte(int offset, byte value) {
        OSMemory.pokeByte(osaddr + offset, value);
    }

    public final void pokeByteArray(int offset, byte[] bytes, int bytesOffset, int length) {
        OSMemory.pokeByteArray(osaddr + offset, bytes, bytesOffset, length);
    }

    public final void pokeShortArray(int offset, short[] shorts,
            int shortsOffset, int length, boolean swap) {
        OSMemory.pokeShortArray(osaddr + offset, shorts, shortsOffset, length, swap);
    }

    public final void pokeIntArray(int offset, int[] ints,
            int intsOffset, int length, boolean swap) {
        OSMemory.pokeIntArray(osaddr + offset, ints, intsOffset, length, swap);
    }

    public final void pokeFloatArray(int offset, float[] floats,
            int floatsOffset, int length, boolean swap) {
        OSMemory.pokeFloatArray(osaddr + offset, floats, floatsOffset, length, swap);
    }

    public final byte peekByte(int offset) {
        return OSMemory.peekByte(osaddr + offset);
    }

    public final void peekByteArray(int offset, byte[] bytes, int bytesOffset, int length) {
        OSMemory.peekByteArray(osaddr + offset, bytes, bytesOffset, length);
    }

    public final void pokeShort(int offset, short value, ByteOrder order) {
        OSMemory.pokeShort(osaddr + offset, value, order.needsSwap);
    }

    public final short peekShort(int offset, ByteOrder order) {
        return OSMemory.peekShort(osaddr + offset, order.needsSwap);
    }

    public final void pokeInt(int offset, int value, ByteOrder order) {
        OSMemory.pokeInt(osaddr + offset, value, order.needsSwap);
    }

    public final int peekInt(int offset, ByteOrder order) {
        return OSMemory.peekInt(osaddr + offset, order.needsSwap);
    }

    public final void pokeLong(int offset, long value, ByteOrder order) {
        OSMemory.pokeLong(osaddr + offset, value, order.needsSwap);
    }

    public final long peekLong(int offset, ByteOrder order) {
        return OSMemory.peekLong(osaddr + offset, order.needsSwap);
    }

    public final void pokeFloat(int offset, float value, ByteOrder order) {
        OSMemory.pokeInt(osaddr + offset, Float.floatToRawIntBits(value), order.needsSwap);
    }

    public final float peekFloat(int offset, ByteOrder order) {
        return Float.intBitsToFloat(OSMemory.peekInt(osaddr + offset, order.needsSwap));
    }

    public final void pokeDouble(int offset, double value, ByteOrder order) {
        OSMemory.pokeLong(osaddr + offset, Double.doubleToRawLongBits(value), order.needsSwap);
    }

    public final double peekDouble(int offset, ByteOrder order) {
        return Double.longBitsToDouble(OSMemory.peekLong(osaddr + offset, order.needsSwap));
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
}
