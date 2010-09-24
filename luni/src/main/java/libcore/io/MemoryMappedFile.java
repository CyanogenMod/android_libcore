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

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.NioUtils;
import java.nio.channels.FileChannel;
import org.apache.harmony.luni.platform.OSMemory;

/**
 * A memory-mapped file. Use {@link #mmap} to map a file, {@link #close} to unmap a file,
 * and either {@link #bigEndianIterator} or {@link #littleEndianIterator} to get a seekable
 * {@link BufferIterator} over the mapped data.
 */
public final class MemoryMappedFile implements Closeable {
    private int address;

    // Until we have 64-bit address spaces, we only need an int for 'size'.
    private final int size;

    private MemoryMappedFile(int address, int size) {
        this.address = address;
        this.size = size;
    }

    public static MemoryMappedFile mmap(FileChannel fc, FileChannel.MapMode mapMode, long start, long size) throws IOException {
        return mmap(NioUtils.getFd(fc), mapMode, start, size);
    }

    public static MemoryMappedFile mmap(FileDescriptor fd, FileChannel.MapMode mapMode, long start, long size) throws IOException {
        return mmap(IoUtils.getFd(fd), mapMode, start, size);
    }

    private static MemoryMappedFile mmap(int fd, FileChannel.MapMode mapMode, long start, long size) throws IOException {
        if (start < 0) {
            throw new IllegalArgumentException("start < 0: " + start);
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size <= 0: " + size);
        }
        if ((start + size) > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("(start + size) > Integer.MAX_VALUE");
        }
        int address = OSMemory.mmap(fd, start, size, mapMode);
        return new MemoryMappedFile(address, (int) size);
    }

    /**
     * Unmaps this memory-mapped file using munmap(2). This is a no-op if close has already been
     * called. Note that this class does <i>not</i> use finalization; you must call {@code close}
     * yourself.
     *
     * Calling this method invalidates any iterators over this {@code MemoryMappedFile}. It is an
     * error to use such an iterator after calling {@code close}.
     */
    public synchronized void close() throws IOException {
        if (address != 0) {
            OSMemory.munmap(address, size);
            address = 0;
        }
    }

    /**
     * Returns a new iterator that treats the mapped data as big-endian.
     */
    public BufferIterator bigEndianIterator() {
        return new BufferIterator(address, size, ByteOrder.nativeOrder() != ByteOrder.BIG_ENDIAN);
    }

    /**
     * Returns a new iterator that treats the mapped data as little-endian.
     */
    public BufferIterator littleEndianIterator() {
        return new BufferIterator(address, size, ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Returns the size in bytes of the memory-mapped region.
     */
    public int size() {
        return size;
    }
}
