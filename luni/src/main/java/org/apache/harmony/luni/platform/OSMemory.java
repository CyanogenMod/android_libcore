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

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel.MapMode;

/**
 * This class enables direct access to memory.
 *
 * @hide - we should move this in with the NIO stuff it supports, and make it package-private again
 */
public final class OSMemory {
    private OSMemory() { }

    /**
     * Used to optimize nio heap buffer bulk get operations. 'dst' must be a primitive array.
     * 'dstOffset' is measured in units of 'sizeofElements' bytes.
     */
    public static native void unsafeBulkGet(Object dst, int dstOffset, int byteCount,
            byte[] src, int srcOffset, int sizeofElements, boolean swap);

    /**
     * Used to optimize nio heap buffer bulk put operations. 'src' must be a primitive array.
     * 'srcOffset' is measured in units of 'sizeofElements' bytes.
     */
    public static native void unsafeBulkPut(byte[] dst, int dstOffset, int byteCount,
            Object src, int srcOffset, int sizeofElements, boolean swap);

    public static int peekInt(byte[] src, int offset, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            return (((src[offset++] & 0xff) << 24) |
                    ((src[offset++] & 0xff) << 16) |
                    ((src[offset++] & 0xff) <<  8) |
                    ((src[offset  ] & 0xff) <<  0));
        } else {
            return (((src[offset++] & 0xff) <<  0) |
                    ((src[offset++] & 0xff) <<  8) |
                    ((src[offset++] & 0xff) << 16) |
                    ((src[offset  ] & 0xff) << 24));
        }
    }

    public static long peekLong(byte[] src, int offset, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            int h = ((src[offset++] & 0xff) << 24) |
                    ((src[offset++] & 0xff) << 16) |
                    ((src[offset++] & 0xff) <<  8) |
                    ((src[offset++] & 0xff) <<  0);
            int l = ((src[offset++] & 0xff) << 24) |
                    ((src[offset++] & 0xff) << 16) |
                    ((src[offset++] & 0xff) <<  8) |
                    ((src[offset  ] & 0xff) <<  0);
            return (((long) h) << 32L) | ((long) l) & 0xffffffffL;
        } else {
            int l = ((src[offset++] & 0xff) <<  0) |
                    ((src[offset++] & 0xff) <<  8) |
                    ((src[offset++] & 0xff) << 16) |
                    ((src[offset++] & 0xff) << 24);
            int h = ((src[offset++] & 0xff) <<  0) |
                    ((src[offset++] & 0xff) <<  8) |
                    ((src[offset++] & 0xff) << 16) |
                    ((src[offset  ] & 0xff) << 24);
            return (((long) h) << 32L) | ((long) l) & 0xffffffffL;
        }
    }

    public static short peekShort(byte[] src, int offset, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            return (short) ((src[offset] << 8) | (src[offset + 1] & 0xff));
        } else {
            return (short) ((src[offset + 1] << 8) | (src[offset] & 0xff));
        }
    }

    public static void pokeInt(byte[] dst, int offset, int value, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            dst[offset++] = (byte) ((value >> 24) & 0xff);
            dst[offset++] = (byte) ((value >> 16) & 0xff);
            dst[offset++] = (byte) ((value >>  8) & 0xff);
            dst[offset  ] = (byte) ((value >>  0) & 0xff);
        } else {
            dst[offset++] = (byte) ((value >>  0) & 0xff);
            dst[offset++] = (byte) ((value >>  8) & 0xff);
            dst[offset++] = (byte) ((value >> 16) & 0xff);
            dst[offset  ] = (byte) ((value >> 24) & 0xff);
        }
    }

    public static void pokeLong(byte[] dst, int offset, long value, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            int i = (int) (value >> 32);
            dst[offset++] = (byte) ((i >> 24) & 0xff);
            dst[offset++] = (byte) ((i >> 16) & 0xff);
            dst[offset++] = (byte) ((i >>  8) & 0xff);
            dst[offset++] = (byte) ((i >>  0) & 0xff);
            i = (int) value;
            dst[offset++] = (byte) ((i >> 24) & 0xff);
            dst[offset++] = (byte) ((i >> 16) & 0xff);
            dst[offset++] = (byte) ((i >>  8) & 0xff);
            dst[offset  ] = (byte) ((i >>  0) & 0xff);
        } else {
            int i = (int) value;
            dst[offset++] = (byte) ((i >>  0) & 0xff);
            dst[offset++] = (byte) ((i >>  8) & 0xff);
            dst[offset++] = (byte) ((i >> 16) & 0xff);
            dst[offset++] = (byte) ((i >> 24) & 0xff);
            i = (int) (value >> 32);
            dst[offset++] = (byte) ((i >>  0) & 0xff);
            dst[offset++] = (byte) ((i >>  8) & 0xff);
            dst[offset++] = (byte) ((i >> 16) & 0xff);
            dst[offset  ] = (byte) ((i >> 24) & 0xff);
        }
    }

    public static void pokeShort(byte[] dst, int offset, short value, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            dst[offset++] = (byte) ((value >> 8) & 0xff);
            dst[offset  ] = (byte) ((value >> 0) & 0xff);
        } else {
            dst[offset++] = (byte) ((value >> 0) & 0xff);
            dst[offset  ] = (byte) ((value >> 8) & 0xff);
        }
    }

    /**
     * Returns the address of byteCount bytes of memory. Unlike the corresponding C library
     * function, the memory returned has been zero-initialized.
     */
    public static native int malloc(int byteCount) throws OutOfMemoryError;

    /**
     * Deallocates space for a memory block that was previously allocated by a
     * call to {@link #malloc(int) malloc(int)}. The number of bytes freed is
     * identical to the number of bytes acquired when the memory block was
     * allocated. If <code>address</code> is zero the method does nothing.
     * <p>
     * Freeing a pointer to a memory block that was not allocated by
     * <code>malloc()</code> has unspecified effect.
     * </p>
     *
     * @param address
     *            the address of the memory block to deallocate.
     */
    public static native void free(int address);

    /**
     * Copies <code>length</code> bytes from <code>srcAddress</code> to
     * <code>destAddress</code>. Where any part of the source memory block
     * and the destination memory block overlap <code>memmove()</code> ensures
     * that the original source bytes in the overlapping region are copied
     * before being overwritten.
     * <p>
     * The behavior is unspecified if
     * <code>(srcAddress ... srcAddress + length)</code> and
     * <code>(destAddress ... destAddress + length)</code> are not both wholly
     * within the range that was previously allocated using
     * <code>malloc()</code>.
     * </p>
     *
     * @param destAddress
     *            the address of the destination memory block.
     * @param srcAddress
     *            the address of the source memory block.
     * @param length
     *            the number of bytes to move.
     */
    public static native void memmove(int destAddress, int srcAddress, long length);

    public static native byte peekByte(int address);
    public static native int peekInt(int address, boolean swap);
    public static native long peekLong(int address, boolean swap);
    public static native short peekShort(int address, boolean swap);

    public static native void peekByteArray(int address, byte[] dst, int dstOffset, int byteCount);
    public static native void peekCharArray(int address, char[] dst, int dstOffset, int charCount, boolean swap);
    public static native void peekDoubleArray(int address, double[] dst, int dstOffset, int doubleCount, boolean swap);
    public static native void peekFloatArray(int address, float[] dst, int dstOffset, int floatCount, boolean swap);
    public static native void peekIntArray(int address, int[] dst, int dstOffset, int intCount, boolean swap);
    public static native void peekLongArray(int address, long[] dst, int dstOffset, int longCount, boolean swap);
    public static native void peekShortArray(int address, short[] dst, int dstOffset, int shortCount, boolean swap);

    public static native void pokeByte(int address, byte value);
    public static native void pokeInt(int address, int value, boolean swap);
    public static native void pokeLong(int address, long value, boolean swap);
    public static native void pokeShort(int address, short value, boolean swap);

    public static native void pokeByteArray(int address, byte[] src, int offset, int count);
    public static native void pokeCharArray(int address, char[] src, int offset, int count, boolean swap);
    public static native void pokeDoubleArray(int address, double[] src, int offset, int count, boolean swap);
    public static native void pokeFloatArray(int address, float[] src, int offset, int count, boolean swap);
    public static native void pokeIntArray(int address, int[] src, int offset, int count, boolean swap);
    public static native void pokeLongArray(int address, long[] src, int offset, int count, boolean swap);
    public static native void pokeShortArray(int address, short[] src, int offset, int count, boolean swap);

    private static native int mmapImpl(int fd, long offset, long size, int mapMode);

    public static int mmap(int fd, long offset, long size, MapMode mapMode) throws IOException {
        // Check just those errors mmap(2) won't detect.
        if (offset < 0 || size < 0 || offset > Integer.MAX_VALUE || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("offset=" + offset + " size=" + size);
        }
        int intMode = 0; // MapMode.PRIVATE
        if (mapMode == MapMode.READ_ONLY) {
            intMode = 1;
        } else if (mapMode == MapMode.READ_WRITE) {
            intMode = 2;
        }
        return mmapImpl(fd, offset, size, intMode);
    }

    public static native void munmap(int addr, long size);

    public static native void load(int addr, long size);

    public static native boolean isLoaded(int addr, long size);

    public static native void msync(int addr, long size);
}
