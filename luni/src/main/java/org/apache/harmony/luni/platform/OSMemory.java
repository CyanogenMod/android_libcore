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
 */
final class OSMemory {
    private OSMemory() { }

    /**
     * Allocates and returns a pointer to space for a memory block of
     * <code>length</code> bytes. The space is uninitialized and may be larger
     * than the number of bytes requested; however, the guaranteed usable memory
     * block is exactly <code>length</code> bytes int.
     *
     * @param length
     *            number of bytes requested.
     * @return the address of the start of the memory block.
     * @throws OutOfMemoryError
     *             if the request cannot be satisfied.
     */
    public static native int malloc(int length) throws OutOfMemoryError;

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
     * Places <code>value</code> into first <code>length</code> bytes of the
     * memory block starting at <code>address</code>.
     * <p>
     * The behavior is unspecified if
     * <code>(address ... address + length)</code> is not wholly within the
     * range that was previously allocated using <code>malloc()</code>.
     * </p>
     *
     * @param address
     *            the address of the first memory location.
     * @param value
     *            the byte value to set at each location.
     * @param length
     *            the number of byte-length locations to set.
     */
    public static native void memset(int address, byte value, long length);

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
    public static native float peekFloat(int address, boolean swap);
    public static native double peekDouble(int address, boolean swap);
    public static native int peekInt(int address, boolean swap);
    public static native long peekLong(int address, boolean swap);
    public static native short peekShort(int address, boolean swap);

    public static native void peekByteArray(int address, byte[] bytes, int offset, int length);

    public static native void pokeByte(int address, byte value);
    public static native void pokeDouble(int address, double value, boolean swap);
    public static native void pokeFloat(int address, float value, boolean swap);
    public static native void pokeInt(int address, int value, boolean swap);
    public static native void pokeLong(int address, long value, boolean swap);
    public static native void pokeShort(int address, short value, boolean swap);

    public static native void pokeByteArray(int address, byte[] src, int offset, int length);
    public static native void pokeFloatArray(int address, float[] src, int offset, int length,
            boolean swap);
    public static native void pokeIntArray(int address, int[] src, int offset, int length,
            boolean swap);
    public static native void pokeShortArray(int address, short[] src, int offset, int length,
            boolean swap);

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
