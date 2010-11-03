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

import libcore.io.SizeOf;
import org.apache.harmony.luni.platform.OSMemory;

/**
 * HeapByteBuffer, ReadWriteHeapByteBuffer and ReadOnlyHeapByteBuffer compose
 * the implementation of array based byte buffers.
 * <p>
 * HeapByteBuffer implements all the shared readonly methods and is extended by
 * the other two classes.
 * </p>
 * <p>
 * All methods are marked final for runtime performance.
 * </p>
 *
 */
abstract class HeapByteBuffer extends BaseByteBuffer {

    protected final byte[] backingArray;

    protected final int offset;

    HeapByteBuffer(byte[] backingArray) {
        this(backingArray, backingArray.length, 0);
    }

    HeapByteBuffer(int capacity) {
        this(new byte[capacity], capacity, 0);
    }

    HeapByteBuffer(byte[] backingArray, int capacity, int offset) {
        super(capacity, null);
        this.backingArray = backingArray;
        this.offset = offset;

        if (offset + capacity > backingArray.length) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public final ByteBuffer get(byte[] dst, int dstOffset, int byteCount) {
        checkGetBounds(1, dst.length, dstOffset, byteCount);
        System.arraycopy(backingArray, offset + position, dst, dstOffset, byteCount);
        position += byteCount;
        return this;
    }

    final void get(char[] dst, int dstOffset, int charCount) {
        int byteCount = checkGetBounds(SizeOf.CHAR, dst.length, dstOffset, charCount);
        OSMemory.unsafeBulkGet(dst, dstOffset, byteCount, backingArray, offset + position, SizeOf.CHAR, order.needsSwap);
        position += byteCount;
    }

    final void get(double[] dst, int dstOffset, int doubleCount) {
        int byteCount = checkGetBounds(SizeOf.DOUBLE, dst.length, dstOffset, doubleCount);
        OSMemory.unsafeBulkGet(dst, dstOffset, byteCount, backingArray, offset + position, SizeOf.DOUBLE, order.needsSwap);
        position += byteCount;
    }

    final void get(float[] dst, int dstOffset, int floatCount) {
        int byteCount = checkGetBounds(SizeOf.FLOAT, dst.length, dstOffset, floatCount);
        OSMemory.unsafeBulkGet(dst, dstOffset, byteCount, backingArray, offset + position, SizeOf.FLOAT, order.needsSwap);
        position += byteCount;
    }

    final void get(int[] dst, int dstOffset, int intCount) {
        int byteCount = checkGetBounds(SizeOf.INT, dst.length, dstOffset, intCount);
        OSMemory.unsafeBulkGet(dst, dstOffset, byteCount, backingArray, offset + position, SizeOf.INT, order.needsSwap);
        position += byteCount;
    }

    final void get(long[] dst, int dstOffset, int longCount) {
        int byteCount = checkGetBounds(SizeOf.LONG, dst.length, dstOffset, longCount);
        OSMemory.unsafeBulkGet(dst, dstOffset, byteCount, backingArray, offset + position, SizeOf.LONG, order.needsSwap);
        position += byteCount;
    }

    final void get(short[] dst, int dstOffset, int shortCount) {
        int byteCount = checkGetBounds(SizeOf.SHORT, dst.length, dstOffset, shortCount);
        OSMemory.unsafeBulkGet(dst, dstOffset, byteCount, backingArray, offset + position, SizeOf.SHORT, order.needsSwap);
        position += byteCount;
    }

    @Override
    public final byte get() {
        if (position == limit) {
            throw new BufferUnderflowException();
        }
        return backingArray[offset + position++];
    }

    @Override
    public final byte get(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        return backingArray[offset + index];
    }

    @Override
    public final char getChar() {
        int newPosition = position + SizeOf.CHAR;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        char result = (char) OSMemory.peekShort(backingArray, offset + position, order);
        position = newPosition;
        return result;
    }

    @Override
    public final char getChar(int index) {
        if (index < 0 || index + SizeOf.CHAR > limit) {
            throw new IndexOutOfBoundsException();
        }
        return (char) OSMemory.peekShort(backingArray, offset + index, order);
    }

    @Override
    public final double getDouble() {
        return Double.longBitsToDouble(getLong());
    }

    @Override
    public final double getDouble(int index) {
        return Double.longBitsToDouble(getLong(index));
    }

    @Override
    public final float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    @Override
    public final float getFloat(int index) {
        return Float.intBitsToFloat(getInt(index));
    }

    @Override
    public final int getInt() {
        int newPosition = position + SizeOf.INT;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        int result = OSMemory.peekInt(backingArray, offset + position, order);
        position = newPosition;
        return result;
    }

    @Override
    public final int getInt(int index) {
        if (index < 0 || index + SizeOf.INT > limit) {
            throw new IndexOutOfBoundsException();
        }
        return OSMemory.peekInt(backingArray, offset + index, order);
    }

    @Override
    public final long getLong() {
        int newPosition = position + SizeOf.LONG;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        long result = OSMemory.peekLong(backingArray, offset + position, order);
        position = newPosition;
        return result;
    }

    @Override
    public final long getLong(int index) {
        if (index < 0 || index + SizeOf.LONG > limit) {
            throw new IndexOutOfBoundsException();
        }
        return OSMemory.peekLong(backingArray, offset + index, order);
    }

    @Override
    public final short getShort() {
        int newPosition = position + SizeOf.SHORT;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        short result = OSMemory.peekShort(backingArray, offset + position, order);
        position = newPosition;
        return result;
    }

    @Override
    public final short getShort(int index) {
        if (index < 0 || index + SizeOf.SHORT > limit) {
            throw new IndexOutOfBoundsException();
        }
        return OSMemory.peekShort(backingArray, offset + index, order);
    }

    @Override
    public final boolean isDirect() {
        return false;
    }
}
