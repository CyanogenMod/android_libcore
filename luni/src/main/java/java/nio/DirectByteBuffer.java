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

abstract class DirectByteBuffer extends BaseByteBuffer implements DirectBuffer {
    protected final MemoryBlock block;

    // This is the offset into 'block' at which this buffer logically starts.
    // TODO: rewrite this to use an OffsetMemoryBlock?
    protected final int offset;

    protected DirectByteBuffer(MemoryBlock block, int capacity, int offset) {
        super(capacity);

        long baseSize = block.getSize();
        if (baseSize >= 0 && (capacity + offset) > baseSize) {
            throw new IllegalArgumentException("capacity + offset > baseSize");
        }

        this.block = block;
        this.offset = offset;
        this.effectiveDirectAddress = block.toInt() + offset;
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
        this.block.peekByteArray(offset + position, dst, off, len);
        position += len;
        return this;
    }

    @Override
    public final byte get() {
        if (position == limit) {
            throw new BufferUnderflowException();
        }
        return this.block.peekByte(offset + position++);
    }

    @Override
    public final byte get(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        return this.block.peekByte(offset + index);
    }

    @Override
    public final double getDouble() {
        int newPosition = position + SIZEOF_DOUBLE;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        double result = this.block.peekDouble(offset + position, order);
        position = newPosition;
        return result;
    }

    @Override
    public final double getDouble(int index) {
        if (index < 0 || (long) index + SIZEOF_DOUBLE > limit) {
            throw new IndexOutOfBoundsException();
        }
        return this.block.peekDouble(offset + index, order);
    }

    @Override
    public final float getFloat() {
        int newPosition = position + SIZEOF_FLOAT;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        float result = this.block.peekFloat(offset + position, order);
        position = newPosition;
        return result;
    }

    @Override
    public final float getFloat(int index) {
        if (index < 0 || (long) index + SIZEOF_FLOAT > limit) {
            throw new IndexOutOfBoundsException();
        }
        return this.block.peekFloat(offset + index, order);
    }

    @Override
    public final int getInt() {
        int newPosition = position + SIZEOF_INT;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        int result = this.block.peekInt(offset + position, order);
        position = newPosition;
        return result;
    }

    @Override
    public final int getInt(int index) {
        if (index < 0 || (long) index + SIZEOF_INT > limit) {
            throw new IndexOutOfBoundsException();
        }
        return this.block.peekInt(offset + index, order);
    }

    @Override
    public final long getLong() {
        int newPosition = position + SIZEOF_LONG;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        long result = this.block.peekLong(offset + position, order);
        position = newPosition;
        return result;
    }

    @Override
    public final long getLong(int index) {
        if (index < 0 || (long) index + SIZEOF_LONG > limit) {
            throw new IndexOutOfBoundsException();
        }
        return this.block.peekLong(offset + index, order);
    }

    @Override
    public final short getShort() {
        int newPosition = position + SIZEOF_SHORT;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        short result = this.block.peekShort(offset + position, order);
        position = newPosition;
        return result;
    }

    @Override
    public final short getShort(int index) {
        if (index < 0 || (long) index + SIZEOF_SHORT > limit) {
            throw new IndexOutOfBoundsException();
        }
        return this.block.peekShort(offset + index, order);
    }

    @Override
    public final boolean isDirect() {
        return true;
    }

    public final MemoryBlock getBaseAddress() {
        return block;
    }

    public final void free() {
        block.free();
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
}
