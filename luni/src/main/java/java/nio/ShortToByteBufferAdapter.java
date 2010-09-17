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
 * This class wraps a byte buffer to be a short buffer.
 * <p>
 * Implementation notice:
 * <ul>
 * <li>After a byte buffer instance is wrapped, it becomes privately owned by
 * the adapter. It must NOT be accessed outside the adapter any more.</li>
 * <li>The byte buffer's position and limit are NOT linked with the adapter.
 * The adapter extends Buffer, thus has its own position and limit.</li>
 * </ul>
 * </p>
 */
final class ShortToByteBufferAdapter extends ShortBuffer {

    static ShortBuffer wrap(ByteBuffer byteBuffer) {
        return new ShortToByteBufferAdapter(byteBuffer.slice());
    }

    private final ByteBuffer byteBuffer;

    ShortToByteBufferAdapter(ByteBuffer byteBuffer) {
        super(byteBuffer.capacity() / SIZEOF_SHORT);
        this.byteBuffer = byteBuffer;
        this.byteBuffer.clear();
        this.effectiveDirectAddress = byteBuffer.effectiveDirectAddress;
    }

    @Override
    public ShortBuffer asReadOnlyBuffer() {
        ShortToByteBufferAdapter buf = new ShortToByteBufferAdapter(byteBuffer.asReadOnlyBuffer());
        buf.limit = limit;
        buf.position = position;
        buf.mark = mark;
        return buf;
    }

    @Override
    public ShortBuffer compact() {
        if (byteBuffer.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        byteBuffer.limit(limit * SIZEOF_SHORT);
        byteBuffer.position(position * SIZEOF_SHORT);
        byteBuffer.compact();
        byteBuffer.clear();
        position = limit - position;
        limit = capacity;
        mark = UNSET_MARK;
        return this;
    }

    @Override
    public ShortBuffer duplicate() {
        ShortToByteBufferAdapter buf = new ShortToByteBufferAdapter(byteBuffer.duplicate());
        buf.limit = limit;
        buf.position = position;
        buf.mark = mark;
        return buf;
    }

    @Override
    public short get() {
        if (position == limit) {
            throw new BufferUnderflowException();
        }
        return byteBuffer.getShort(position++ * SIZEOF_SHORT);
    }

    @Override
    public short get(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        return byteBuffer.getShort(index * SIZEOF_SHORT);
    }

    @Override
    public ShortBuffer get(short[] dst, int dstOffset, int shortCount) {
        byteBuffer.limit(limit * SIZEOF_SHORT);
        byteBuffer.position(position * SIZEOF_SHORT);
        if (byteBuffer instanceof DirectByteBuffer) {
            ((DirectByteBuffer) byteBuffer).get(dst, dstOffset, shortCount);
        } else {
            ((HeapByteBuffer) byteBuffer).get(dst, dstOffset, shortCount);
        }
        this.position += shortCount;
        return this;
    }

    @Override
    public boolean isDirect() {
        return byteBuffer.isDirect();
    }

    @Override
    public boolean isReadOnly() {
        return byteBuffer.isReadOnly();
    }

    @Override
    public ByteOrder order() {
        return byteBuffer.order();
    }

    @Override
    protected short[] protectedArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int protectedArrayOffset() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean protectedHasArray() {
        return false;
    }

    @Override
    public ShortBuffer put(short c) {
        if (position == limit) {
            throw new BufferOverflowException();
        }
        byteBuffer.putShort(position++ * SIZEOF_SHORT, c);
        return this;
    }

    @Override
    public ShortBuffer put(int index, short c) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        byteBuffer.putShort(index * SIZEOF_SHORT, c);
        return this;
    }

    @Override
    public ShortBuffer put(short[] src, int srcOffset, int shortCount) {
        if (byteBuffer instanceof ReadWriteDirectByteBuffer) {
            byteBuffer.limit(limit * SIZEOF_SHORT);
            byteBuffer.position(position * SIZEOF_SHORT);
            ((ReadWriteDirectByteBuffer) byteBuffer).put(src, srcOffset, shortCount);
            this.position += shortCount;
            return this;
        } else {
            return super.put(src, srcOffset, shortCount);
        }
    }

    @Override
    public ShortBuffer slice() {
        byteBuffer.limit(limit * SIZEOF_SHORT);
        byteBuffer.position(position * SIZEOF_SHORT);
        ShortBuffer result = new ShortToByteBufferAdapter(byteBuffer.slice());
        byteBuffer.clear();
        return result;
    }

}
