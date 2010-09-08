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

import org.apache.harmony.luni.platform.OSMemory;

/**
 * DirectByteBuffer, ReadWriteDirectByteBuffer and ReadOnlyDirectByteBuffer
 * compose the implementation of platform memory based byte buffers.
 * <p>
 * ReadWriteDirectByteBuffer extends DirectByteBuffer with all the write
 * methods.
 * </p>
 * <p>
 * This class is marked final for runtime performance.
 * </p>
 */
final class ReadWriteDirectByteBuffer extends DirectByteBuffer {
    static ReadWriteDirectByteBuffer copy(DirectByteBuffer other, int markOfOther) {
        ReadWriteDirectByteBuffer buf = new ReadWriteDirectByteBuffer(other.block, other.capacity(), other.offset);
        buf.limit = other.limit();
        buf.position = other.position();
        buf.mark = markOfOther;
        buf.order(other.order());
        return buf;
    }

    // Used by ByteBuffer.allocateDirect.
    ReadWriteDirectByteBuffer(int capacity) {
        super(MemoryBlock.malloc(capacity), capacity, 0);
    }

    // Used by the JNI NewDirectByteBuffer function.
    ReadWriteDirectByteBuffer(int address, int capacity) {
        super(MemoryBlock.wrapFromJni(address, capacity), capacity, 0);
    }

    ReadWriteDirectByteBuffer(MemoryBlock block, int capacity, int offset) {
        super(block, capacity, offset);
    }

    @Override
    public ByteBuffer asReadOnlyBuffer() {
        return ReadOnlyDirectByteBuffer.copy(this, mark);
    }

    @Override
    public ByteBuffer compact() {
        int addr = effectiveDirectAddress;
        OSMemory.memmove(addr, addr + position, remaining());
        position = limit - position;
        limit = capacity;
        mark = UNSET_MARK;
        return this;
    }

    @Override
    public ByteBuffer duplicate() {
        return copy(this, mark);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public ByteBuffer put(byte value) {
        if (position == limit) {
            throw new BufferOverflowException();
        }
        this.block.pokeByte(offset + position++, value);
        return this;
    }

    @Override
    public ByteBuffer put(int index, byte value) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        this.block.pokeByte(offset + index, value);
        return this;
    }

    /*
     * Override ByteBuffer.put(byte[], int, int) to improve performance.
     *
     * (non-Javadoc)
     *
     * @see java.nio.ByteBuffer#put(byte[], int, int)
     */
    @Override
    public ByteBuffer put(byte[] src, int off, int len) {
        int length = src.length;
        if (off < 0 || len < 0 || (long) off + (long) len > length) {
            throw new IndexOutOfBoundsException();
        }
        if (len > remaining()) {
            throw new BufferOverflowException();
        }
        this.block.pokeByteArray(offset + position, src, off, len);
        position += len;
        return this;
    }

    /**
     * Writes <code>short</code>s in the given short array, starting from the
     * specified offset, to the current position and increase the position by
     * the number of <code>short</code>s written.
     *
     * @param src
     *            The source short array
     * @param off
     *            The offset of short array, must be no less than zero and no
     *            greater than <code>src.length</code>
     * @param len
     *            The number of <code>short</code>s to write, must be no less
     *            than zero and no greater than <code>src.length - off</code>
     * @return This buffer
     * @exception BufferOverflowException
     *                If <code>remaining()</code> is less than
     *                <code>len</code>
     * @exception IndexOutOfBoundsException
     *                If either <code>off</code> or <code>len</code> is
     *                invalid
     * @exception ReadOnlyBufferException
     *                If no changes may be made to the contents of this buffer
     */
    ByteBuffer put(short[] src, int off, int len) {
        int length = src.length;
        if (off < 0 || len < 0 || (long)off + (long)len > length) {
            throw new IndexOutOfBoundsException();
        }
        int byteCount = len * SIZEOF_SHORT;
        if (byteCount > remaining()) {
            throw new BufferOverflowException();
        }
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        this.block.pokeShortArray(offset + position, src, off, len, order.needsSwap);
        position += byteCount;
        return this;
    }

    /**
     * Writes <code>int</code>s in the given int array, starting from the
     * specified offset, to the current position and increase the position by
     * the number of <code>int</code>s written.
     *
     * @param src
     *            The source int array
     * @param off
     *            The offset of int array, must be no less than zero and no
     *            greater than <code>src.length</code>
     * @param len
     *            The number of <code>int</code>s to write, must be no less
     *            than zero and no greater than <code>src.length - off</code>
     * @return This buffer
     * @exception BufferOverflowException
     *                If <code>remaining()</code> is less than
     *                <code>len</code>
     * @exception IndexOutOfBoundsException
     *                If either <code>off</code> or <code>len</code> is
     *                invalid
     * @exception ReadOnlyBufferException
     *                If no changes may be made to the contents of this buffer
     */
    ByteBuffer put(int[] src, int off, int len) {
        int length = src.length;
        if (off < 0 || len < 0 || (long)off + (long)len > length) {
            throw new IndexOutOfBoundsException();
        }
        int byteCount = len * SIZEOF_INT;
        if (byteCount > remaining()) {
            throw new BufferOverflowException();
        }
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        this.block.pokeIntArray(offset + position, src, off, len, order.needsSwap);
        position += byteCount;
        return this;
    }

    /**
     * Writes <code>float</code>s in the given float array, starting from the
     * specified offset, to the current position and increase the position by
     * the number of <code>float</code>s written.
     *
     * @param src
     *            The source float array
     * @param off
     *            The offset of float array, must be no less than zero and no
     *            greater than <code>src.length</code>
     * @param len
     *            The number of <code>float</code>s to write, must be no less
     *            than zero and no greater than <code>src.length - off</code>
     * @return This buffer
     * @exception BufferOverflowException
     *                If <code>remaining()</code> is less than
     *                <code>len</code>
     * @exception IndexOutOfBoundsException
     *                If either <code>off</code> or <code>len</code> is
     *                invalid
     * @exception ReadOnlyBufferException
     *                If no changes may be made to the contents of this buffer
     */
    ByteBuffer put(float[] src, int off, int len) {
        int length = src.length;
        if (off < 0 || len < 0 || (long)off + (long)len > length) {
            throw new IndexOutOfBoundsException();
        }
        int byteCount = len * SIZEOF_FLOAT;
        if (byteCount > remaining()) {
            throw new BufferOverflowException();
        }
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        this.block.pokeFloatArray(offset + position, src, off, len, order.needsSwap);
        position += byteCount;
        return this;
    }

    @Override
    public ByteBuffer putDouble(double value) {
        int newPosition = position + SIZEOF_DOUBLE;
        if (newPosition > limit) {
            throw new BufferOverflowException();
        }
        this.block.pokeDouble(offset + position, value, order);
        position = newPosition;
        return this;
    }

    @Override
    public ByteBuffer putDouble(int index, double value) {
        if (index < 0 || (long) index + SIZEOF_DOUBLE > limit) {
            throw new IndexOutOfBoundsException();
        }
        this.block.pokeDouble(offset + index, value, order);
        return this;
    }

    @Override
    public ByteBuffer putFloat(float value) {
        int newPosition = position + SIZEOF_FLOAT;
        if (newPosition > limit) {
            throw new BufferOverflowException();
        }
        this.block.pokeFloat(offset + position, value, order);
        position = newPosition;
        return this;
    }

    @Override
    public ByteBuffer putFloat(int index, float value) {
        if (index < 0 || (long) index + SIZEOF_FLOAT > limit) {
            throw new IndexOutOfBoundsException();
        }
        this.block.pokeFloat(offset + index, value, order);
        return this;
    }

    @Override
    public ByteBuffer putInt(int value) {
        int newPosition = position + SIZEOF_INT;
        if (newPosition > limit) {
            throw new BufferOverflowException();
        }
        this.block.pokeInt(offset + position, value, order);
        position = newPosition;
        return this;
    }

    @Override
    public ByteBuffer putInt(int index, int value) {
        if (index < 0 || (long) index + SIZEOF_INT > limit) {
            throw new IndexOutOfBoundsException();
        }
        this.block.pokeInt(offset + index, value, order);
        return this;
    }

    @Override
    public ByteBuffer putLong(long value) {
        int newPosition = position + SIZEOF_LONG;
        if (newPosition > limit) {
            throw new BufferOverflowException();
        }
        this.block.pokeLong(offset + position, value, order);
        position = newPosition;
        return this;
    }

    @Override
    public ByteBuffer putLong(int index, long value) {
        if (index < 0 || (long) index + SIZEOF_LONG > limit) {
            throw new IndexOutOfBoundsException();
        }
        this.block.pokeLong(offset + index, value, order);
        return this;
    }

    @Override
    public ByteBuffer putShort(short value) {
        int newPosition = position + SIZEOF_SHORT;
        if (newPosition > limit) {
            throw new BufferOverflowException();
        }
        this.block.pokeShort(offset + position, value, order);
        position = newPosition;
        return this;
    }

    @Override
    public ByteBuffer putShort(int index, short value) {
        if (index < 0 || (long) index + SIZEOF_SHORT > limit) {
            throw new IndexOutOfBoundsException();
        }
        this.block.pokeShort(offset + index, value, order);
        return this;
    }

    @Override
    public ByteBuffer slice() {
        ReadWriteDirectByteBuffer buf = new ReadWriteDirectByteBuffer(block, remaining(), offset + position);
        buf.order = order;
        return buf;
    }

}
