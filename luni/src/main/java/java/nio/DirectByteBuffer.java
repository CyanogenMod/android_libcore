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

abstract class DirectByteBuffer extends BaseByteBuffer {
    // This is the offset into {@code Buffer.block} at which this buffer logically starts.
    // TODO: rewrite this so we set 'block' to an OffsetMemoryBlock?
    protected final int offset;

    protected DirectByteBuffer(MemoryBlock block, int capacity, int offset) {
        super(capacity, block);

        long baseSize = block.getSize();
        if (baseSize >= 0 && (capacity + offset) > baseSize) {
            throw new IllegalArgumentException("capacity + offset > baseSize");
        }

        this.offset = offset;
        this.effectiveDirectAddress = block.toInt() + offset;
    }

    private int checkBounds(int bytesPerElement, int length, int offset, int count) {
        int byteCount = bytesPerElement * count;
        if (offset < 0 || count < 0 || (long) offset + (long) count > length) {
            throw new IndexOutOfBoundsException();
        }
        if (byteCount > remaining()) {
            throw new BufferUnderflowException();
        }
        return byteCount;
    }

    @Override
    public final ByteBuffer get(byte[] dst, int dstOffset, int byteCount) {
        checkBounds(1, dst.length, dstOffset, byteCount);
        this.block.peekByteArray(offset + position, dst, dstOffset, byteCount);
        position += byteCount;
        return this;
    }

    final void get(char[] dst, int dstOffset, int charCount) {
        int byteCount = checkBounds(SIZEOF_CHAR, dst.length, dstOffset, charCount);
        this.block.peekCharArray(offset + position, dst, dstOffset, charCount, order.needsSwap);
        position += byteCount;
    }

    final void get(double[] dst, int dstOffset, int doubleCount) {
        int byteCount = checkBounds(SIZEOF_DOUBLE, dst.length, dstOffset, doubleCount);
        this.block.peekDoubleArray(offset + position, dst, dstOffset, doubleCount, order.needsSwap);
        position += byteCount;
    }

    final void get(float[] dst, int dstOffset, int floatCount) {
        int byteCount = checkBounds(SIZEOF_FLOAT, dst.length, dstOffset, floatCount);
        this.block.peekFloatArray(offset + position, dst, dstOffset, floatCount, order.needsSwap);
        position += byteCount;
    }

    final void get(int[] dst, int dstOffset, int intCount) {
        int byteCount = checkBounds(SIZEOF_INT, dst.length, dstOffset, intCount);
        this.block.peekIntArray(offset + position, dst, dstOffset, intCount, order.needsSwap);
        position += byteCount;
    }

    final void get(long[] dst, int dstOffset, int longCount) {
        int byteCount = checkBounds(SIZEOF_LONG, dst.length, dstOffset, longCount);
        this.block.peekLongArray(offset + position, dst, dstOffset, longCount, order.needsSwap);
        position += byteCount;
    }

    final void get(short[] dst, int dstOffset, int shortCount) {
        int byteCount = checkBounds(SIZEOF_SHORT, dst.length, dstOffset, shortCount);
        this.block.peekShortArray(offset + position, dst, dstOffset, shortCount, order.needsSwap);
        position += byteCount;
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
