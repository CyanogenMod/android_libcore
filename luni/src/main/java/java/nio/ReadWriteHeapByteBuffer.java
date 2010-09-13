/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.nio;

/**
 * HeapByteBuffer, ReadWriteHeapByteBuffer and ReadOnlyHeapByteBuffer compose
 * the implementation of array based byte buffers.
 * <p>
 * ReadWriteHeapByteBuffer extends HeapByteBuffer with all the write methods.
 * </p>
 * <p>
 * This class is marked final for runtime performance.
 * </p>
 *
 */
final class ReadWriteHeapByteBuffer extends HeapByteBuffer {

    static ReadWriteHeapByteBuffer copy(HeapByteBuffer other, int markOfOther) {
        ReadWriteHeapByteBuffer buf = new ReadWriteHeapByteBuffer(other.backingArray,
                other.capacity(), other.offset);
        buf.limit = other.limit;
        buf.position = other.position();
        buf.mark = markOfOther;
        buf.order(other.order());
        return buf;
    }

    ReadWriteHeapByteBuffer(byte[] backingArray) {
        super(backingArray);
    }

    ReadWriteHeapByteBuffer(int capacity) {
        super(capacity);
    }

    ReadWriteHeapByteBuffer(byte[] backingArray, int capacity, int arrayOffset) {
        super(backingArray, capacity, arrayOffset);
    }

    @Override
    public ByteBuffer asReadOnlyBuffer() {
        return ReadOnlyHeapByteBuffer.copy(this, mark);
    }

    @Override
    public ByteBuffer compact() {
        System.arraycopy(backingArray, position + offset, backingArray, offset, remaining());
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
    protected byte[] protectedArray() {
        return backingArray;
    }

    @Override
    protected int protectedArrayOffset() {
        return offset;
    }

    @Override
    protected boolean protectedHasArray() {
        return true;
    }

    @Override
    public ByteBuffer put(byte b) {
        if (position == limit) {
            throw new BufferOverflowException();
        }
        backingArray[offset + position++] = b;
        return this;
    }

    @Override
    public ByteBuffer put(int index, byte b) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        backingArray[offset + index] = b;
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
    public ByteBuffer put(byte[] src, int srcOffset, int byteCount) {
        if (srcOffset < 0 || byteCount < 0 || (long) srcOffset + (long) byteCount > src.length) {
            throw new IndexOutOfBoundsException();
        }
        if (byteCount > remaining()) {
            throw new BufferOverflowException();
        }
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        System.arraycopy(src, srcOffset, backingArray, offset + position, byteCount);
        position += byteCount;
        return this;
    }

    @Override
    public ByteBuffer putChar(int index, char value) {
        if (index < 0 || (long) index + SIZEOF_CHAR > limit) {
            throw new IndexOutOfBoundsException();
        }
        storeShort(index, (short) value);
        return this;
    }

    @Override
    public ByteBuffer putChar(char value) {
        int newPosition = position + SIZEOF_CHAR;
        if (newPosition > limit) {
            throw new BufferOverflowException();
        }
        storeShort(position, (short) value);
        position = newPosition;
        return this;
    }

    @Override
    public ByteBuffer putDouble(double value) {
        return putLong(Double.doubleToRawLongBits(value));
    }

    @Override
    public ByteBuffer putDouble(int index, double value) {
        return putLong(index, Double.doubleToRawLongBits(value));
    }

    @Override
    public ByteBuffer putFloat(float value) {
        return putInt(Float.floatToRawIntBits(value));
    }

    @Override
    public ByteBuffer putFloat(int index, float value) {
        return putInt(index, Float.floatToRawIntBits(value));
    }

    @Override
    public ByteBuffer putInt(int value) {
        int newPosition = position + SIZEOF_INT;
        if (newPosition > limit) {
            throw new BufferOverflowException();
        }
        storeInt(position, value);
        position = newPosition;
        return this;
    }

    @Override
    public ByteBuffer putInt(int index, int value) {
        if (index < 0 || (long) index + SIZEOF_INT > limit) {
            throw new IndexOutOfBoundsException();
        }
        storeInt(index, value);
        return this;
    }

    @Override
    public ByteBuffer putLong(int index, long value) {
        if (index < 0 || (long) index + SIZEOF_LONG > limit) {
            throw new IndexOutOfBoundsException();
        }
        storeLong(index, value);
        return this;
    }

    @Override
    public ByteBuffer putLong(long value) {
        int newPosition = position + SIZEOF_LONG;
        if (newPosition > limit) {
            throw new BufferOverflowException();
        }
        storeLong(position, value);
        position = newPosition;
        return this;
    }

    @Override
    public ByteBuffer putShort(int index, short value) {
        if (index < 0 || (long) index + SIZEOF_SHORT > limit) {
            throw new IndexOutOfBoundsException();
        }
        storeShort(index, value);
        return this;
    }

    @Override
    public ByteBuffer putShort(short value) {
        int newPosition = position + SIZEOF_SHORT;
        if (newPosition > limit) {
            throw new BufferOverflowException();
        }
        storeShort(position, value);
        position = newPosition;
        return this;
    }

    private final void storeInt(int index, int value) {
        int baseOffset = offset + index;
        if (order == ByteOrder.BIG_ENDIAN) {
            backingArray[baseOffset++] = (byte) ((value >> 24) & 0xff);
            backingArray[baseOffset++] = (byte) ((value >> 16) & 0xff);
            backingArray[baseOffset++] = (byte) ((value >>  8) & 0xff);
            backingArray[baseOffset  ] = (byte) ((value >>  0) & 0xff);
        } else {
            backingArray[baseOffset++] = (byte) ((value >>  0) & 0xff);
            backingArray[baseOffset++] = (byte) ((value >>  8) & 0xff);
            backingArray[baseOffset++] = (byte) ((value >> 16) & 0xff);
            backingArray[baseOffset  ] = (byte) ((value >> 24) & 0xff);
        }
    }

    private final void storeLong(int index, long value) {
        int baseOffset = offset + index;
        if (order == ByteOrder.BIG_ENDIAN) {
            int i = (int) (value >> 32);
            backingArray[baseOffset++] = (byte) ((i >> 24) & 0xff);
            backingArray[baseOffset++] = (byte) ((i >> 16) & 0xff);
            backingArray[baseOffset++] = (byte) ((i >>  8) & 0xff);
            backingArray[baseOffset++] = (byte) ((i >>  0) & 0xff);
            i = (int) value;
            backingArray[baseOffset++] = (byte) ((i >> 24) & 0xff);
            backingArray[baseOffset++] = (byte) ((i >> 16) & 0xff);
            backingArray[baseOffset++] = (byte) ((i >>  8) & 0xff);
            backingArray[baseOffset  ] = (byte) ((i >>  0) & 0xff);
        } else {
            int i = (int) value;
            backingArray[baseOffset++] = (byte) ((i >>  0) & 0xff);
            backingArray[baseOffset++] = (byte) ((i >>  8) & 0xff);
            backingArray[baseOffset++] = (byte) ((i >> 16) & 0xff);
            backingArray[baseOffset++] = (byte) ((i >> 24) & 0xff);
            i = (int) (value >> 32);
            backingArray[baseOffset++] = (byte) ((i >>  0) & 0xff);
            backingArray[baseOffset++] = (byte) ((i >>  8) & 0xff);
            backingArray[baseOffset++] = (byte) ((i >> 16) & 0xff);
            backingArray[baseOffset  ] = (byte) ((i >> 24) & 0xff);
        }
    }

    private final void storeShort(int index, short value) {
        int baseOffset = offset + index;
        if (order == ByteOrder.BIG_ENDIAN) {
            backingArray[baseOffset++] = (byte) ((value >> 8) & 0xff);
            backingArray[baseOffset  ] = (byte) ((value >> 0) & 0xff);
        } else {
            backingArray[baseOffset++] = (byte) ((value >> 0) & 0xff);
            backingArray[baseOffset  ] = (byte) ((value >> 8) & 0xff);
        }
    }

    @Override
    public ByteBuffer slice() {
        ReadWriteHeapByteBuffer slice = new ReadWriteHeapByteBuffer(backingArray, remaining(),
                offset + position);
        slice.order = order;
        return slice;
    }
}
