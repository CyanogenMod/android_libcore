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
 * This class wraps a byte buffer to be a long buffer.
 * <p>
 * Implementation notice:
 * <ul>
 * <li>After a byte buffer instance is wrapped, it becomes privately owned by
 * the adapter. It must NOT be accessed outside the adapter any more.</li>
 * <li>The byte buffer's position and limit are NOT linked with the adapter.
 * The adapter extends Buffer, thus has its own position and limit.</li>
 * </ul>
 * </p>
 *
 */
final class LongToByteBufferAdapter extends LongBuffer {

    static LongBuffer wrap(ByteBuffer byteBuffer) {
        return new LongToByteBufferAdapter(byteBuffer.slice());
    }

    private final ByteBuffer byteBuffer;

    LongToByteBufferAdapter(ByteBuffer byteBuffer) {
        super(byteBuffer.capacity() / SIZEOF_LONG);
        this.byteBuffer = byteBuffer;
        this.byteBuffer.clear();
        this.effectiveDirectAddress = byteBuffer.effectiveDirectAddress;
    }

    @Override
    public LongBuffer asReadOnlyBuffer() {
        LongToByteBufferAdapter buf = new LongToByteBufferAdapter(byteBuffer.asReadOnlyBuffer());
        buf.limit = limit;
        buf.position = position;
        buf.mark = mark;
        return buf;
    }

    @Override
    public LongBuffer compact() {
        if (byteBuffer.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        byteBuffer.limit(limit * SIZEOF_LONG);
        byteBuffer.position(position * SIZEOF_LONG);
        byteBuffer.compact();
        byteBuffer.clear();
        position = limit - position;
        limit = capacity;
        mark = UNSET_MARK;
        return this;
    }

    @Override
    public LongBuffer duplicate() {
        LongToByteBufferAdapter buf = new LongToByteBufferAdapter(byteBuffer.duplicate());
        buf.limit = limit;
        buf.position = position;
        buf.mark = mark;
        return buf;
    }

    @Override
    public long get() {
        if (position == limit) {
            throw new BufferUnderflowException();
        }
        return byteBuffer.getLong(position++ * SIZEOF_LONG);
    }

    @Override
    public long get(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        return byteBuffer.getLong(index * SIZEOF_LONG);
    }

    @Override
    public LongBuffer get(long[] dst, int dstOffset, int longCount) {
        if (byteBuffer instanceof DirectByteBuffer) {
            byteBuffer.limit(limit * SIZEOF_LONG);
            byteBuffer.position(position * SIZEOF_LONG);
            ((DirectByteBuffer) byteBuffer).get(dst, dstOffset, longCount);
            this.position += longCount;
            return this;
        } else {
            return super.get(dst, dstOffset, longCount);
        }
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
    protected long[] protectedArray() {
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
    public LongBuffer put(long c) {
        if (position == limit) {
            throw new BufferOverflowException();
        }
        byteBuffer.putLong(position++ * SIZEOF_LONG, c);
        return this;
    }

    @Override
    public LongBuffer put(int index, long c) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        byteBuffer.putLong(index * SIZEOF_LONG, c);
        return this;
    }

    @Override
    public LongBuffer put(long[] src, int srcOffset, int longCount) {
        if (byteBuffer instanceof ReadWriteDirectByteBuffer) {
            byteBuffer.limit(limit * SIZEOF_LONG);
            byteBuffer.position(position * SIZEOF_LONG);
            ((ReadWriteDirectByteBuffer) byteBuffer).put(src, srcOffset, longCount);
            this.position += longCount;
            return this;
        } else {
            return super.put(src, srcOffset, longCount);
        }
    }

    @Override
    public LongBuffer slice() {
        byteBuffer.limit(limit * SIZEOF_LONG);
        byteBuffer.position(position * SIZEOF_LONG);
        LongBuffer result = new LongToByteBufferAdapter(byteBuffer.slice());
        byteBuffer.clear();
        return result;
    }

}
