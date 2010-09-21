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

package java.nio;

import java.io.IOException;
import java.nio.channels.FileChannel.MapMode;
import org.apache.harmony.luni.platform.OSMemory;

class MemoryBlock {
    /**
     * Handles calling munmap(2) on a memory-mapped region.
     */
    private static class MemoryMappedBlock extends MemoryBlock {
        private MemoryMappedBlock(int address, long byteCount) {
            super(address, byteCount);
        }

        @Override public void free() {
            if (address != 0) {
                OSMemory.munmap(address, size);
                address = 0;
            }
        }

        @Override protected void finalize() throws Throwable {
            free();
        }
    }

    /**
     * Handles calling free(3) on a native heap block.
     */
    private static class NativeHeapBlock extends MemoryBlock {
        private NativeHeapBlock(int address, long byteCount) {
            super(address, byteCount);
        }

        @Override public void free() {
            if (address != 0) {
                OSMemory.free(address);
                address = 0;
            }
        }

        @Override protected void finalize() throws Throwable {
            free();
        }
    }

    /**
     * Represents a block of memory we don't own. (We don't take ownership of memory corresponding
     * to direct buffers created by the JNI NewDirectByteBuffer function.)
     */
    private static class UnmanagedBlock extends MemoryBlock {
        private UnmanagedBlock(int address, long byteCount) {
            super(address, byteCount);
        }
    }

    // TODO: should be long on 64-bit devices; int for performance.
    protected int address;
    protected final long size;

    public static MemoryBlock mmap(int fd, long start, long size, MapMode mode) throws IOException {
        if (size == 0) {
            // You can't mmap(2) a zero-length region.
            return new MemoryBlock(0, 0);
        }
        int address = OSMemory.mmap(fd, start, size, mode);
        return new MemoryMappedBlock(address, size);
    }

    public static MemoryBlock malloc(int byteCount) {
        return new NativeHeapBlock(OSMemory.malloc(byteCount), byteCount);
    }

    public static MemoryBlock wrapFromJni(int address, long byteCount) {
        return new UnmanagedBlock(address, byteCount);
    }

    private MemoryBlock(int address, long size) {
        this.address = address;
        this.size = size;
    }

    public void free() {
    }

    public final void pokeByte(int offset, byte value) {
        OSMemory.pokeByte(address + offset, value);
    }

    public final void pokeByteArray(int offset, byte[] src, int srcOffset, int byteCount) {
        OSMemory.pokeByteArray(address + offset, src, srcOffset, byteCount);
    }

    public final void pokeCharArray(int offset, char[] src, int srcOffset, int charCount, boolean swap) {
        OSMemory.pokeCharArray(address + offset, src, srcOffset, charCount, swap);
    }

    public final void pokeDoubleArray(int offset, double[] src, int srcOffset, int doubleCount, boolean swap) {
        OSMemory.pokeDoubleArray(address + offset, src, srcOffset, doubleCount, swap);
    }

    public final void pokeFloatArray(int offset, float[] src, int srcOffset, int floatCount, boolean swap) {
        OSMemory.pokeFloatArray(address + offset, src, srcOffset, floatCount, swap);
    }

    public final void pokeIntArray(int offset, int[] src, int srcOffset, int intCount, boolean swap) {
        OSMemory.pokeIntArray(address + offset, src, srcOffset, intCount, swap);
    }

    public final void pokeLongArray(int offset, long[] src, int srcOffset, int longCount, boolean swap) {
        OSMemory.pokeLongArray(address + offset, src, srcOffset, longCount, swap);
    }

    public final void pokeShortArray(int offset, short[] src, int srcOffset, int shortCount, boolean swap) {
        OSMemory.pokeShortArray(address + offset, src, srcOffset, shortCount, swap);
    }

    public final byte peekByte(int offset) {
        return OSMemory.peekByte(address + offset);
    }

    public final void peekByteArray(int offset, byte[] dst, int dstOffset, int byteCount) {
        OSMemory.peekByteArray(address + offset, dst, dstOffset, byteCount);
    }

    public final void peekCharArray(int offset, char[] dst, int dstOffset, int charCount, boolean swap) {
        OSMemory.peekCharArray(address + offset, dst, dstOffset, charCount, swap);
    }

    public final void peekDoubleArray(int offset, double[] dst, int dstOffset, int doubleCount, boolean swap) {
        OSMemory.peekDoubleArray(address + offset, dst, dstOffset, doubleCount, swap);
    }

    public final void peekFloatArray(int offset, float[] dst, int dstOffset, int floatCount, boolean swap) {
        OSMemory.peekFloatArray(address + offset, dst, dstOffset, floatCount, swap);
    }

    public final void peekIntArray(int offset, int[] dst, int dstOffset, int intCount, boolean swap) {
        OSMemory.peekIntArray(address + offset, dst, dstOffset, intCount, swap);
    }

    public final void peekLongArray(int offset, long[] dst, int dstOffset, int longCount, boolean swap) {
        OSMemory.peekLongArray(address + offset, dst, dstOffset, longCount, swap);
    }

    public final void peekShortArray(int offset, short[] dst, int dstOffset, int shortCount, boolean swap) {
        OSMemory.peekShortArray(address + offset, dst, dstOffset, shortCount, swap);
    }

    public final void pokeShort(int offset, short value, ByteOrder order) {
        OSMemory.pokeShort(address + offset, value, order.needsSwap);
    }

    public final short peekShort(int offset, ByteOrder order) {
        return OSMemory.peekShort(address + offset, order.needsSwap);
    }

    public final void pokeInt(int offset, int value, ByteOrder order) {
        OSMemory.pokeInt(address + offset, value, order.needsSwap);
    }

    public final int peekInt(int offset, ByteOrder order) {
        return OSMemory.peekInt(address + offset, order.needsSwap);
    }

    public final void pokeLong(int offset, long value, ByteOrder order) {
        OSMemory.pokeLong(address + offset, value, order.needsSwap);
    }

    public final long peekLong(int offset, ByteOrder order) {
        return OSMemory.peekLong(address + offset, order.needsSwap);
    }

    public final int toInt() {
        return address;
    }

    public final String toString() {
        return getClass().getName() + "[" + address + "]";
    }

    public final long getSize() {
        return size;
    }
}
