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
import org.apache.harmony.luni.platform.PlatformAddress;
import org.apache.harmony.nio.internal.DirectBuffer;

/**
 * DirectByteBuffer, ReadWriteDirectByteBuffer and ReadOnlyDirectByteBuffer
 * compose the implementation of platform memory based byte buffers.
 * <p>
 * DirectByteBuffer implements all the shared readonly methods and is extended
 * by the other two classes.
 * </p>
 * <p>
 * All methods are marked final for runtime performance.
 * </p>
 *
 */
abstract class DirectByteBuffer extends BaseByteBuffer implements DirectBuffer {

    // This is a reference to the base address of the buffer memory.
    protected final PlatformAddress address;

    // This is the offset from the base address at which this buffer logically
    // starts.
    protected final int offset;

    protected DirectByteBuffer(PlatformAddress address, int capacity, int offset) {
        super(capacity);

        // BEGIN android-added
        long baseSize = address.getSize();
        if ((baseSize >= 0) && ((offset + capacity) > baseSize)) {
            throw new IllegalArgumentException("slice out of range");
        }
        // END android-added

        this.address = address;
        this.offset = offset;
    }

    /*
     * Override ByteBuffer.get(byte[], int, int) to improve performance.
     *
     * (non-Javadoc)
     *
     * @see java.nio.ByteBuffer#get(byte[], int, int)
     */
    @Override
    public final ByteBuffer get(byte[] dst, int off, int len) {
        int length = dst.length;
        if ((off < 0) || (len < 0) || (long) off + (long) len > length) {
            throw new IndexOutOfBoundsException();
        }
        if (len > remaining()) {
            throw new BufferUnderflowException();
        }
        getBaseAddress().peekByteArray(offset + position, dst, off, len);
        position += len;
        return this;
    }

    @Override
    public final byte get() {
        if (position == limit) {
            throw new BufferUnderflowException();
        }
        return getBaseAddress().peekByte(offset + position++);
    }

    @Override
    public final byte get(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        return getBaseAddress().peekByte(offset + index);
    }

    @Override
    public final double getDouble() {
        int newPosition = position + SIZEOF_DOUBLE;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        double result = getBaseAddress().peekDouble(offset + position, order);
        position = newPosition;
        return result;
    }

    @Override
    public final double getDouble(int index) {
        if (index < 0 || (long) index + SIZEOF_DOUBLE > limit) {
            throw new IndexOutOfBoundsException();
        }
        return getBaseAddress().peekDouble(offset + index, order);
    }

    @Override
    public final float getFloat() {
        int newPosition = position + SIZEOF_FLOAT;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        float result = getBaseAddress().peekFloat(offset + position, order);
        position = newPosition;
        return result;
    }

    @Override
    public final float getFloat(int index) {
        if (index < 0 || (long) index + SIZEOF_FLOAT > limit) {
            throw new IndexOutOfBoundsException();
        }
        return getBaseAddress().peekFloat(offset + index, order);
    }

    @Override
    public final int getInt() {
        int newPosition = position + SIZEOF_INT;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        int result = getBaseAddress().peekInt(offset + position, order);
        position = newPosition;
        return result;
    }

    @Override
    public final int getInt(int index) {
        if (index < 0 || (long) index + SIZEOF_INT > limit) {
            throw new IndexOutOfBoundsException();
        }
        return getBaseAddress().peekInt(offset + index, order);
    }

    @Override
    public final long getLong() {
        int newPosition = position + SIZEOF_LONG;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        long result = getBaseAddress().peekLong(offset + position, order);
        position = newPosition;
        return result;
    }

    @Override
    public final long getLong(int index) {
        if (index < 0 || (long) index + SIZEOF_LONG > limit) {
            throw new IndexOutOfBoundsException();
        }
        return getBaseAddress().peekLong(offset + index, order);
    }

    @Override
    public final short getShort() {
        int newPosition = position + SIZEOF_SHORT;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        short result = getBaseAddress().peekShort(offset + position, order);
        position = newPosition;
        return result;
    }

    @Override
    public final short getShort(int index) {
        if (index < 0 || (long) index + SIZEOF_SHORT > limit) {
            throw new IndexOutOfBoundsException();
        }
        return getBaseAddress().peekShort(offset + index, order);
    }

    @Override
    public final boolean isDirect() {
        return true;
    }

    /*
     * Returns the base address of the buffer (i.e. before offset).
     */
    public final PlatformAddress getBaseAddress() {
        return address;
    }

    /**
     * Returns the platform address of the start of this buffer instance.
     * <em>You must not attempt to free the returned address!!</em> It may not
     * be an address that was explicitly malloc'ed (i.e. if this buffer is the
     * result of a split); and it may be memory shared by multiple buffers.
     * <p>
     * If you can guarantee that you want to free the underlying memory call the
     * #free() method on this instance -- generally applications will rely on
     * the garbage collector to autofree this memory.
     * </p>
     *
     * @return the effective address of the start of the buffer.
     * @throws IllegalStateException
     *             if this buffer address is known to have been freed
     *             previously.
     */
    public final int getEffectiveAddress() {
        effectiveDirectAddress =  getBaseAddress().toInt() + offset;
        return effectiveDirectAddress;
    }

    public final void free() {
        address.free();
    }

    @Override
    final protected byte[] protectedArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    final protected int protectedArrayOffset() {
        throw new UnsupportedOperationException();
    }

    @Override
    final protected boolean protectedHasArray() {
        return false;
    }

    public final int getByteCapacity() {
        return capacity;
    }
}
