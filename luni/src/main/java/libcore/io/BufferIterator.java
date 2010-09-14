/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libcore.io;

import org.apache.harmony.luni.platform.OSMemory;

/**
 * Iterates over big- or little-endian bytes. See {@link MemoryMappedFile#bigEndianIterator} and
 * {@link MemoryMappedFile#littleEndianIterator}.
 *
 * @hide don't make this public without adding bounds checking.
 */
public final class BufferIterator {
    private final int address;
    private final int size;
    private final boolean swap;

    private int position;

    BufferIterator(int address, int size, boolean swap) {
        this.address = address;
        this.size = size;
        this.swap = swap;
    }

    /**
     * Skips forwards or backwards {@code byteCount} bytes from the current position.
     */
    public void skip(int byteCount) {
        position += byteCount;
    }

    /**
     * Copies {@code byteCount} bytes from the current position into {@code dst}, starting at
     * {@code dstOffset}, and advances the current position {@code byteCount} bytes.
     */
    public void readByteArray(byte[] dst, int dstOffset, int byteCount) {
        OSMemory.peekByteArray(address + position, dst, dstOffset, byteCount);
        position += byteCount;
    }

    /**
     * Returns the byte at the current position, and advances the current position one byte.
     */
    public byte readByte() {
        byte result = OSMemory.peekByte(address + position);
        ++position;
        return result;
    }

    /**
     * Returns the 32-bit int at the current position, and advances the current position four bytes.
     */
    public int readInt() {
        int result = OSMemory.peekInt(address + position, swap);
        position += 4;
        return result;
    }

    /**
     * Copies {@code intCount} 32-bit ints from the current position into {@code dst}, starting at
     * {@code dstOffset}, and advances the current position {@code 4 * intCount} bytes.
     */
    public void readIntArray(int[] dst, int dstOffset, int intCount) {
        OSMemory.peekIntArray(address + position, dst, dstOffset, intCount, swap);
        position += 4 * intCount;
    }
}
