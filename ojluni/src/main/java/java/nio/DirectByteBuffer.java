/*
 * Copyright (c) 2000, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.nio;

import java.io.FileDescriptor;
import sun.misc.Cleaner;
import sun.misc.Unsafe;
import sun.misc.VM;
import sun.nio.ch.DirectBuffer;
import libcore.io.SizeOf;
import libcore.io.Memory;
import dalvik.system.VMRuntime;

class DirectByteBuffer extends MappedByteBuffer
    implements DirectBuffer {

    //there is no use of it in the class. Remove it after making
    //changes in the child classes.
    protected static final Unsafe unsafe = Bits.unsafe();

    // Cached unaligned-access capability
    private static Boolean unalignedCache;
    protected static boolean unaligned() {
        if (unalignedCache == null) {
            unalignedCache = Bits.unaligned();
        }
        return unalignedCache;
    }

    // Base address, used in all indexing calculations
    // NOTE: moved up to Buffer.java for speed in JNI GetDirectBufferAddress
    //    protected long address;

    // An object attached to this buffer. If this buffer is a view of another
    // buffer then we use this field to keep a reference to that buffer to
    // ensure that its memory isn't freed before we are done with it.
    private final Object att;

    private boolean accessible = true;
    private boolean freed = false;


    public Object attachment() {
        return att;
    }

    private class Deallocator implements Runnable {

        public void run() {
            free();
        }
    }

    private Cleaner cleaner;

    public Cleaner cleaner() { return cleaner; }

    DirectByteBuffer(int capacity) {
        super(-1, 0, capacity, capacity, (byte[]) VMRuntime.getRuntime()
              .newNonMovableArray(byte.class, capacity), 0);
        VMRuntime runtime = VMRuntime.getRuntime();
        address = runtime.addressOf(hb);
        cleaner = Cleaner.create(this, new Deallocator());
        this.isReadOnly = false;
        att = null;
    }

    DirectByteBuffer(long addr, int cap, Object ob) {
        super(-1, 0, cap, cap);
        address = addr;
        cleaner = null;
        att = ob;
    }

    // Invoked only by JNI: NewDirectByteBuffer(void*, long)
    //
    private DirectByteBuffer(long addr, int cap) {
        super(-1, 0, cap, cap);
        address = addr;
        cleaner = null;
        att = null;
    }

    // For memory-mapped buffers -- invoked by FileChannelImpl via reflection
    //
    protected DirectByteBuffer(int cap, long addr,
                               FileDescriptor fd,
                               Runnable unmapper) {
        this(cap, addr, fd, unmapper, false);
    }

    protected DirectByteBuffer(int cap, long addr,
                               FileDescriptor fd,
                               Runnable unmapper,
                               boolean isReadOnly) {
        super(-1, 0, cap, cap, fd);
        this.isReadOnly = isReadOnly;
        address = addr;
        cleaner = Cleaner.create(this, unmapper);
        att = null;
    }

    // For duplicates and slices
    //
    DirectByteBuffer(DirectBuffer db,         // package-private
                     int mark, int pos, int lim, int cap,
                     int off) {
        this(db, mark, pos, lim, cap, off, false);
    }

    DirectByteBuffer(DirectBuffer db,         // package-private
                     int mark, int pos, int lim, int cap,
                     int off, boolean isReadOnly) {
        super(mark, pos, lim, cap);
        this.isReadOnly = isReadOnly;
        address = db.address() + off;
        cleaner = null;
        att = db;
    }

    public ByteBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        int off = (pos << 0);
        assert (off >= 0);
        return new DirectByteBuffer(this, -1, 0, rem, rem, off, isReadOnly);
    }

    public ByteBuffer duplicate() {
        return new DirectByteBuffer(this,
                                    this.markValue(),
                                    this.position(),
                                    this.limit(),
                                    this.capacity(),
                                    0,
                                    isReadOnly);
    }

    public ByteBuffer asReadOnlyBuffer() {

        return new DirectByteBuffer(this,
                                    this.markValue(),
                                    this.position(),
                                    this.limit(),
                                    this.capacity(),
                                    0,
                                    true);
    }

    public long address() {
        return address;
    }

    private long ix(int i) {
        return address + (i << 0);
    }

    public byte get(long a) {
        return Memory.peekByte(a);
    }

    public byte get() {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        return get(address + nextGetIndex());
    }

    public byte get(int i) {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        return get(address + checkIndex(i));
    }

    public ByteBuffer get(byte[] dst, int dstOffset, int length) {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        checkBounds(dstOffset, length, dst.length);
        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        if (length > rem)
            throw new BufferUnderflowException();
        Memory.peekByteArray((int) address + pos,
                             dst, dstOffset, length);
        position = pos + length;
        return this;
    }

    public ByteBuffer put(long a, byte x) {
        Memory.pokeByte(a, x);
        return this;
    }

    public ByteBuffer put(byte x) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        put(ix(nextPutIndex()), x);
        return this;
    }

    public ByteBuffer put(int i, byte x) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        put(ix(checkIndex(i)), x);
        return this;
    }

    public ByteBuffer put(ByteBuffer src) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        if (src instanceof DirectByteBuffer) {
            if (src == this)
                throw new IllegalArgumentException();
            DirectByteBuffer sb = (DirectByteBuffer)src;
            byte[] arr = sb.array();
            put(arr, src.offset, arr.length);
        } else if (src.hb != null) {
            int spos = src.position();
            int slim = src.limit();
            assert (spos <= slim);
            int srem = (spos <= slim ? slim - spos : 0);
            put(src.hb, src.offset + spos, srem);
            src.position(spos + srem);
        } else {
            super.put(src);
        }
        return this;
    }

    public ByteBuffer put(byte[] src, int srcOffset, int length) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        checkBounds(srcOffset, length, src.length);
        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        if (length > rem)
            throw new BufferOverflowException();
        Memory.pokeByteArray((int) address + pos,
                             src, srcOffset, length);
        position = pos + length;
        return this;
    }

    public ByteBuffer compact() {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        Memory.memmove(this, 0, this, position, remaining());
        position(rem);
        limit(capacity());
        discardMark();
        return this;
    }

    public boolean isDirect() {
        return true;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    byte _get(int i) {                          // package-private
        return get(i);
    }

    void _put(int i, byte b) {                  // package-private
        put(i, b);
    }

    private char getChar(long a) {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        return (char) Memory.peekShort(position, !nativeByteOrder);
    }

    public char getChar() {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        int newPosition = position + SizeOf.CHAR;
        if (newPosition > limit()) {
            throw new BufferUnderflowException();
        }
        char x = (char) Memory.peekShort(address + position, !nativeByteOrder);
        position = newPosition;
        return x;
    }

    public char getChar(int i) {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        checkIndex(i, SizeOf.CHAR);
        char x = (char)Memory.peekShort(address + i, !nativeByteOrder);
        return x;
    }

    private ByteBuffer putChar(long a, char x) {
        Memory.pokeShort(a, (short) x, !nativeByteOrder);
        return this;
    }

    public ByteBuffer putChar(char x) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        putChar(ix(nextPutIndex(SizeOf.CHAR)), x);
        return this;
    }

    public ByteBuffer putChar(int i, char x) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        putChar(ix(checkIndex(i, SizeOf.CHAR)), x);
        return this;
    }

    public CharBuffer asCharBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        int size = rem >> 1;
        return  (CharBuffer)(new ByteBufferAsCharBuffer(this,
                                                        -1,
                                                        0,
                                                        size,
                                                        size,
                                                        off,
                                                        order()));
    }

    private short getShort(long a) {
        return Memory.peekShort(a, !nativeByteOrder);
    }

    public short getShort() {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        return getShort(ix(nextGetIndex(SizeOf.SHORT)));
    }

    public short getShort(int i) {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        return getShort(ix(checkIndex(i, SizeOf.SHORT)));
    }

    private ByteBuffer putShort(long a, short x) {
        Memory.pokeShort(a, x, !nativeByteOrder);
        return this;
    }

    public ByteBuffer putShort(short x) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        putShort(ix(nextPutIndex(SizeOf.SHORT)), x);
        return this;
    }

    public ByteBuffer putShort(int i, short x) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        putShort(ix(checkIndex(i, SizeOf.SHORT)), x);
        return this;
    }

    public ShortBuffer asShortBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        int size = rem >> 1;
        return (ShortBuffer)(new ByteBufferAsShortBuffer(this,
                                                         -1,
                                                         0,
                                                         size,
                                                         size,
                                                         off,
                                                         order()));
    }

    private int getInt(long a) {
        return  Memory.peekInt(a, !nativeByteOrder);
    }

    public int getInt() {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        return getInt(ix(nextGetIndex(SizeOf.INT)));
    }

    public int getInt(int i) {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        return getInt(ix(checkIndex(i, (SizeOf.INT))));
    }

    private ByteBuffer putInt(long a, int x) {
        Memory.pokeInt(a, x, !nativeByteOrder);
        return this;
    }

    public ByteBuffer putInt(int x) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        putInt(ix(nextPutIndex(SizeOf.INT)), x);
        return this;
    }

    public ByteBuffer putInt(int i, int x) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        putInt(ix(checkIndex(i, SizeOf.INT)), x);
        return this;
    }

    public IntBuffer asIntBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        int size = rem >> 2;
        return (IntBuffer)(new ByteBufferAsIntBuffer(this,
                                                     -1,
                                                     0,
                                                     size,
                                                     size,
                                                     off,
                                                     order()));
    }

    private long getLong(long a) {
        return Memory.peekLong(a, !nativeByteOrder);
    }

    public long getLong() {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        return getLong(ix(nextGetIndex(SizeOf.LONG)));
    }

    public long getLong(int i) {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        return getLong(ix(checkIndex(i, SizeOf.LONG)));
    }

    private ByteBuffer putLong(long a, long x) {
        Memory.pokeLong(a, x, !nativeByteOrder);
        return this;
    }

    public ByteBuffer putLong(long x) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        putLong(ix(nextPutIndex(SizeOf.LONG)), x);
        return this;
    }

    public ByteBuffer putLong(int i, long x) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        putLong(ix(checkIndex(i, SizeOf.LONG)), x);
        return this;
    }

    public LongBuffer asLongBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        int size = rem >> 3;
        return (LongBuffer)(new ByteBufferAsLongBuffer(this,
                                                       -1,
                                                       0,
                                                       size,
                                                       size,
                                                       off,
                                                       order()));
    }

    private float getFloat(long a) {
        int x = Memory.peekInt(a, !nativeByteOrder);
        return Float.intBitsToFloat(x);
    }

    public float getFloat() {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        return getFloat(ix(nextGetIndex(SizeOf.FLOAT)));
    }

    public float getFloat(int i) {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        return getFloat(ix(checkIndex(i, SizeOf.FLOAT)));
    }

    private ByteBuffer putFloat(long a, float x) {
        int y = Float.floatToRawIntBits(x);
        Memory.pokeInt(a, y, !nativeByteOrder);
        return this;
    }

    public ByteBuffer putFloat(float x) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        putFloat(ix(nextPutIndex(SizeOf.FLOAT)), x);
        return this;
    }

    public ByteBuffer putFloat(int i, float x) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        putFloat(ix(checkIndex(i, SizeOf.FLOAT)), x);
        return this;
    }

    public FloatBuffer asFloatBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        int size = rem >> 2;
        return (FloatBuffer)(new ByteBufferAsFloatBuffer(this,
                                                         -1,
                                                         0,
                                                         size,
                                                         size,
                                                         off,
                                                         order()));
    }

    private double getDouble(long a) {
        long x = Memory.peekLong(a, !nativeByteOrder);
        return Double.longBitsToDouble(x);
    }

    public double getDouble() {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        return getDouble(ix(nextGetIndex(SizeOf.DOUBLE)));
    }

    public double getDouble(int i) {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        return getDouble(ix(checkIndex(i, SizeOf.DOUBLE)));
    }

    private ByteBuffer putDouble(long a, double x) {
        long y = Double.doubleToRawLongBits(x);
        Memory.pokeLong(a, y, !nativeByteOrder);
        return this;
    }

    public ByteBuffer putDouble(double x) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        putDouble(ix(nextPutIndex(SizeOf.DOUBLE)), x);
        return this;
    }

    public ByteBuffer putDouble(int i, double x) {
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
        putDouble(ix(checkIndex(i, SizeOf.DOUBLE)), x);
        return this;
    }

    public DoubleBuffer asDoubleBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);

        int size = rem >> 3;
        return (DoubleBuffer)(new ByteBufferAsDoubleBuffer(this,
                                                           -1,
                                                           0,
                                                           size,
                                                           size,
                                                           off,
                                                           order()));
    }

    public final void free() {
        freed = true;
    }

    private final void checkIfFreed() {
        if (freed) {
            throw new IllegalStateException("buffer was freed");
        }
    }
}
