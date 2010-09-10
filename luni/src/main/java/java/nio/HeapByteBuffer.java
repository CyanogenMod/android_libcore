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

    protected final int loadInt(int index) {
        int baseOffset = offset + index;
        int bytes = 0;
        if (order == ByteOrder.BIG_ENDIAN) {
            for (int i = 0; i < SIZEOF_INT; ++i) {
                bytes = bytes << 8;
                bytes = bytes | (backingArray[baseOffset + i] & 0xFF);
            }
        } else {
            for (int i = SIZEOF_INT - 1; i >= 0; --i) {
                bytes = bytes << 8;
                bytes = bytes | (backingArray[baseOffset + i] & 0xFF);
            }
        }
        return bytes;
    }

    protected final long loadLong(int index) {
        int baseOffset = offset + index;
        long bytes = 0;
        if (order == ByteOrder.BIG_ENDIAN) {
            for (int i = 0; i < SIZEOF_LONG; ++i) {
                bytes = bytes << 8;
                bytes = bytes | (backingArray[baseOffset + i] & 0xFF);
            }
        } else {
            for (int i = SIZEOF_LONG - 1; i >= 0; --i) {
                bytes = bytes << 8;
                bytes = bytes | (backingArray[baseOffset + i] & 0xFF);
            }
        }
        return bytes;
    }

    protected final short loadShort(int index) {
        int baseOffset = offset + index;
        short bytes = 0;
        if (order == ByteOrder.BIG_ENDIAN) {
            bytes = (short) (backingArray[baseOffset] << 8);
            bytes |= (backingArray[baseOffset + 1] & 0xFF);
        } else {
            bytes = (short) (backingArray[baseOffset + 1] << 8);
            bytes |= (backingArray[baseOffset] & 0xFF);
        }
        return bytes;
    }

    protected final void store(int index, int value) {
        int baseOffset = offset + index;
        if (order == ByteOrder.BIG_ENDIAN) {
            for (int i = 3; i >= 0; i--) {
                backingArray[baseOffset + i] = (byte) (value & 0xFF);
                value = value >> 8;
            }
        } else {
            for (int i = 0; i <= 3; i++) {
                backingArray[baseOffset + i] = (byte) (value & 0xFF);
                value = value >> 8;
            }
        }
    }

    protected final void store(int index, long value) {
        int baseOffset = offset + index;
        if (order == ByteOrder.BIG_ENDIAN) {
            for (int i = 7; i >= 0; i--) {
                backingArray[baseOffset + i] = (byte) (value & 0xFF);
                value = value >> 8;
            }
        } else {
            for (int i = 0; i <= 7; i++) {
                backingArray[baseOffset + i] = (byte) (value & 0xFF);
                value = value >> 8;
            }
        }
    }

    protected final void store(int index, short value) {
        int baseOffset = offset + index;
        if (order == ByteOrder.BIG_ENDIAN) {
            backingArray[baseOffset] = (byte) ((value >> 8) & 0xFF);
            backingArray[baseOffset + 1] = (byte) (value & 0xFF);
        } else {
            backingArray[baseOffset + 1] = (byte) ((value >> 8) & 0xFF);
            backingArray[baseOffset] = (byte) (value & 0xFF);
        }
    }
}
