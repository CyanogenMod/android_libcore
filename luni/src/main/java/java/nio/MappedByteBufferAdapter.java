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

import java.nio.channels.FileChannel.MapMode;
import org.apache.harmony.luni.platform.PlatformAddress;
import org.apache.harmony.nio.internal.DirectBuffer;

/**
 * @hide
 */
public final class MappedByteBufferAdapter extends MappedByteBuffer implements DirectBuffer {
    public MappedByteBufferAdapter(ByteBuffer buffer) {
        super(buffer);
    }

    public MappedByteBufferAdapter(PlatformAddress addr, int capacity, int offset, MapMode mode) {
        super(addr, capacity, offset, mode);
    }

    @Override
    public CharBuffer asCharBuffer() {
        return this.wrapped.asCharBuffer();
    }

    @Override
    public DoubleBuffer asDoubleBuffer() {
        return this.wrapped.asDoubleBuffer();
    }

    @Override
    public FloatBuffer asFloatBuffer() {
        return this.wrapped.asFloatBuffer();
    }

    @Override
    public IntBuffer asIntBuffer() {
        return this.wrapped.asIntBuffer();
    }

    @Override
    public LongBuffer asLongBuffer() {
        return this.wrapped.asLongBuffer();
    }

    @Override
    public ByteBuffer asReadOnlyBuffer() {
        MappedByteBufferAdapter buf = new MappedByteBufferAdapter(this.wrapped.asReadOnlyBuffer());
        buf.limit = this.limit;
        buf.position = this.position;
        buf.mark = this.mark;
        return buf;
    }

    @Override
    public ShortBuffer asShortBuffer() {
        return this.wrapped.asShortBuffer();
    }

    @Override
    public ByteBuffer compact() {
        if (this.wrapped.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.compact();
        this.wrapped.clear();
        this.position = this.limit - this.position;
        this.limit = this.capacity;
        this.mark = UNSET_MARK;
        return this;
    }

    @Override
    public ByteBuffer duplicate() {
        MappedByteBufferAdapter buf = new MappedByteBufferAdapter(this.wrapped.duplicate());
        buf.limit = this.limit;
        buf.position = this.position;
        buf.mark = this.mark;
        return buf;
    }

    @Override
    public byte get() {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        byte result = this.wrapped.get();
        this.position++;
        return result;
    }

    @Override
    public byte get(int index) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        return this.wrapped.get(index);
    }

    @Override
    public char getChar() {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        char result = this.wrapped.getChar();
        this.position += SIZEOF_CHAR;
        return result;
    }

    @Override
    public char getChar(int index) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        return this.wrapped.getChar(index);
    }

    @Override
    public double getDouble() {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        double result = this.wrapped.getDouble();
        this.position += SIZEOF_DOUBLE;
        return result;
    }

    @Override
    public double getDouble(int index) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        return this.wrapped.getDouble(index);
    }

    public int getEffectiveAddress() {
        effectiveDirectAddress = ((DirectBuffer) this.wrapped).getEffectiveAddress();
        return effectiveDirectAddress;
    }

    @Override
    public float getFloat() {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        float result = this.wrapped.getFloat();
        this.position += SIZEOF_FLOAT;
        return result;
    }

    @Override
    public float getFloat(int index) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        return this.wrapped.getFloat(index);
    }

    @Override
    public int getInt() {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        int result = this.wrapped.getInt();
        this.position += SIZEOF_INT;
        return result;
    }

    @Override
    public int getInt(int index) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        return this.wrapped.getInt(index);
    }

    @Override
    public long getLong() {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        long result = this.wrapped.getLong();
        this.position += SIZEOF_LONG;
        return result;
    }

    @Override
    public long getLong(int index) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        return this.wrapped.getLong(index);
    }

    @Override
    public short getShort() {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        short result = this.wrapped.getShort();
        this.position += SIZEOF_SHORT;
        return result;
    }

    @Override
    public short getShort(int index) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        return this.wrapped.getShort(index);
    }

    @Override
    public boolean isDirect() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return this.wrapped.isReadOnly();
    }

    @Override
    ByteBuffer orderImpl(ByteOrder byteOrder) {
        super.orderImpl(byteOrder);
        return this.wrapped.order(byteOrder);
    }

    @Override
    public ByteBuffer put(byte b) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.put(b);
        this.position++;
        return this;
    }

    @Override
    public ByteBuffer put(byte[] src, int off, int len) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.put(src, off, len);
        this.position += len;
        return this;
    }

    @Override
    public ByteBuffer put(int index, byte b) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.put(index, b);
        return this;
    }

    @Override
    public ByteBuffer putChar(char value) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.putChar(value);
        this.position += SIZEOF_CHAR;
        return this;
    }

    @Override
    public ByteBuffer putChar(int index, char value) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.putChar(index, value);
        return this;
    }

    @Override
    public ByteBuffer putDouble(double value) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.putDouble(value);
        this.position += SIZEOF_DOUBLE;
        return this;
    }

    @Override
    public ByteBuffer putDouble(int index, double value) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.putDouble(index, value);
        return this;
    }

    @Override
    public ByteBuffer putFloat(float value) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.putFloat(value);
        this.position += SIZEOF_FLOAT;
        return this;
    }

    @Override
    public ByteBuffer putFloat(int index, float value) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.putFloat(index, value);
        return this;
    }

    @Override
    public ByteBuffer putInt(int index, int value) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.putInt(index, value);
        return this;
    }

    @Override
    public ByteBuffer putInt(int value) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.putInt(value);
        this.position += SIZEOF_INT;
        return this;
    }

    @Override
    public ByteBuffer putLong(int index, long value) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.putLong(index, value);
        return this;
    }

    @Override
    public ByteBuffer putLong(long value) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.putLong(value);
        this.position += SIZEOF_LONG;
        return this;
    }

    @Override
    public ByteBuffer putShort(int index, short value) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.putShort(index, value);
        return this;
    }

    @Override
    public ByteBuffer putShort(short value) {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        this.wrapped.putShort(value);
        this.position += SIZEOF_SHORT;
        return this;
    }

    @Override
    public ByteBuffer slice() {
        this.wrapped.limit(this.limit);
        this.wrapped.position(this.position);
        MappedByteBufferAdapter result = new MappedByteBufferAdapter(this.wrapped.slice());
        this.wrapped.clear();
        return result;
    }

    @Override
    byte[] protectedArray() {
        return this.wrapped.protectedArray();
    }

    @Override
    int protectedArrayOffset() {
        return this.wrapped.protectedArrayOffset();
    }

    @Override
    boolean protectedHasArray() {
        return this.wrapped.protectedHasArray();
    }

    public PlatformAddress getBaseAddress() {
        return this.wrapped.getBaseAddress();
    }

    public final void free() {
        this.wrapped.free();
    }
}
