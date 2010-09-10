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
 * This class wraps a byte buffer to be a double buffer.
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
final class DoubleToByteBufferAdapter extends DoubleBuffer {

    static DoubleBuffer wrap(ByteBuffer byteBuffer) {
        return new DoubleToByteBufferAdapter(byteBuffer.slice());
    }

    private final ByteBuffer byteBuffer;

    DoubleToByteBufferAdapter(ByteBuffer byteBuffer) {
        super(byteBuffer.capacity() / SIZEOF_DOUBLE);
        this.byteBuffer = byteBuffer;
        this.byteBuffer.clear();
        this.effectiveDirectAddress = byteBuffer.effectiveDirectAddress;
    }

    @Override
    public DoubleBuffer asReadOnlyBuffer() {
        DoubleToByteBufferAdapter buf = new DoubleToByteBufferAdapter(byteBuffer.asReadOnlyBuffer());
        buf.limit = limit;
        buf.position = position;
        buf.mark = mark;
        return buf;
    }

    @Override
    public DoubleBuffer compact() {
        if (byteBuffer.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        byteBuffer.limit(limit * SIZEOF_DOUBLE);
        byteBuffer.position(position * SIZEOF_DOUBLE);
        byteBuffer.compact();
        byteBuffer.clear();
        position = limit - position;
        limit = capacity;
        mark = UNSET_MARK;
        return this;
    }

    @Override
    public DoubleBuffer duplicate() {
        DoubleToByteBufferAdapter buf = new DoubleToByteBufferAdapter(byteBuffer.duplicate());
        buf.limit = limit;
        buf.position = position;
        buf.mark = mark;
        return buf;
    }

    @Override
    public double get() {
        if (position == limit) {
            throw new BufferUnderflowException();
        }
        return byteBuffer.getDouble(position++ * SIZEOF_DOUBLE);
    }

    @Override
    public double get(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        return byteBuffer.getDouble(index * SIZEOF_DOUBLE);
    }

    @Override
    public DoubleBuffer get(double[] dst, int dstOffset, int doubleCount) {
        if (byteBuffer instanceof DirectByteBuffer) {
            byteBuffer.limit(limit * SIZEOF_DOUBLE);
            byteBuffer.position(position * SIZEOF_DOUBLE);
            ((DirectByteBuffer) byteBuffer).get(dst, dstOffset, doubleCount);
            this.position += doubleCount;
            return this;
        } else {
            return super.get(dst, dstOffset, doubleCount);
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
    protected double[] protectedArray() {
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
    public DoubleBuffer put(double c) {
        if (position == limit) {
            throw new BufferOverflowException();
        }
        byteBuffer.putDouble(position++ * SIZEOF_DOUBLE, c);
        return this;
    }

    @Override
    public DoubleBuffer put(int index, double c) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        byteBuffer.putDouble(index * SIZEOF_DOUBLE, c);
        return this;
    }

    @Override
    public DoubleBuffer put(double[] src, int srcOffset, int doubleCount) {
        if (byteBuffer instanceof ReadWriteDirectByteBuffer) {
            byteBuffer.limit(limit * SIZEOF_DOUBLE);
            byteBuffer.position(position * SIZEOF_DOUBLE);
            ((ReadWriteDirectByteBuffer) byteBuffer).put(src, srcOffset, doubleCount);
            this.position += doubleCount;
            return this;
        } else {
            return super.put(src, srcOffset, doubleCount);
        }
    }

    @Override
    public DoubleBuffer slice() {
        byteBuffer.limit(limit * SIZEOF_DOUBLE);
        byteBuffer.position(position * SIZEOF_DOUBLE);
        DoubleBuffer result = new DoubleToByteBufferAdapter(byteBuffer.slice());
        byteBuffer.clear();
        return result;
    }

}
