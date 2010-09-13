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
        int length = dst.length;
        if (dstOffset < 0 || byteCount < 0 || (long) dstOffset + (long) byteCount > length) {
            throw new IndexOutOfBoundsException();
        }
        if (byteCount > remaining()) {
            throw new BufferUnderflowException();
        }
        System.arraycopy(backingArray, offset + position, dst, dstOffset, byteCount);
        position += byteCount;
        return this;
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
        int newPosition = position + SIZEOF_CHAR;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        char result = (char) loadShort(position);
        position = newPosition;
        return result;
    }

    @Override
    public final char getChar(int index) {
        if (index < 0 || index + SIZEOF_CHAR > limit) {
            throw new IndexOutOfBoundsException();
        }
        return (char) loadShort(index);
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
        int newPosition = position + SIZEOF_INT;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        int result = loadInt(position);
        position = newPosition;
        return result;
    }

    @Override
    public final int getInt(int index) {
        if (index < 0 || index + SIZEOF_INT > limit) {
            throw new IndexOutOfBoundsException();
        }
        return loadInt(index);
    }

    @Override
    public final long getLong() {
        int newPosition = position + SIZEOF_LONG;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        long result = loadLong(position);
        position = newPosition;
        return result;
    }

    @Override
    public final long getLong(int index) {
        if (index < 0 || index + SIZEOF_LONG > limit) {
            throw new IndexOutOfBoundsException();
        }
        return loadLong(index);
    }

    @Override
    public final short getShort() {
        int newPosition = position + SIZEOF_SHORT;
        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }
        short result = loadShort(position);
        position = newPosition;
        return result;
    }

    @Override
    public final short getShort(int index) {
        if (index < 0 || index + SIZEOF_SHORT > limit) {
            throw new IndexOutOfBoundsException();
        }
        return loadShort(index);
    }

    @Override
    public final boolean isDirect() {
        return false;
    }

    private final int loadInt(int index) {
        int baseOffset = offset + index;
        if (order == ByteOrder.BIG_ENDIAN) {
            return (((backingArray[baseOffset++] & 0xff) << 24) |
                    ((backingArray[baseOffset++] & 0xff) << 16) |
                    ((backingArray[baseOffset++] & 0xff) <<  8) |
                    ((backingArray[baseOffset  ] & 0xff) <<  0));
        } else {
            return (((backingArray[baseOffset++] & 0xff) <<  0) |
                    ((backingArray[baseOffset++] & 0xff) <<  8) |
                    ((backingArray[baseOffset++] & 0xff) << 16) |
                    ((backingArray[baseOffset  ] & 0xff) << 24));
        }
    }

    private final long loadLong(int index) {
        int baseOffset = offset + index;
        if (order == ByteOrder.BIG_ENDIAN) {
            int h = ((backingArray[baseOffset++] & 0xff) << 24) |
                    ((backingArray[baseOffset++] & 0xff) << 16) |
                    ((backingArray[baseOffset++] & 0xff) <<  8) |
                    ((backingArray[baseOffset++] & 0xff) <<  0);
            int l = ((backingArray[baseOffset++] & 0xff) << 24) |
                    ((backingArray[baseOffset++] & 0xff) << 16) |
                    ((backingArray[baseOffset++] & 0xff) <<  8) |
                    ((backingArray[baseOffset  ] & 0xff) <<  0);
            return (((long) h) << 32) | l;
        } else {
            int l = ((backingArray[baseOffset++] & 0xff) <<  0) |
                    ((backingArray[baseOffset++] & 0xff) <<  8) |
                    ((backingArray[baseOffset++] & 0xff) << 16) |
                    ((backingArray[baseOffset++] & 0xff) << 24);
            int h = ((backingArray[baseOffset++] & 0xff) <<  0) |
                    ((backingArray[baseOffset++] & 0xff) <<  8) |
                    ((backingArray[baseOffset++] & 0xff) << 16) |
                    ((backingArray[baseOffset  ] & 0xff) << 24);
            return (((long) h) << 32) | l;
        }
    }

    private final short loadShort(int index) {
        int baseOffset = offset + index;
        if (order == ByteOrder.BIG_ENDIAN) {
            return (short)
                    ((backingArray[baseOffset] << 8) | (backingArray[baseOffset + 1] & 0xff));
        } else {
            return (short)
                    ((backingArray[baseOffset + 1] << 8) | (backingArray[baseOffset] & 0xff));
        }
    }
}
