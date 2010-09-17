/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libcore.java.nio;

import java.nio.*;
import java.util.Arrays;
import junit.framework.TestCase;

public class BufferTest extends TestCase {
    public void testByteSwappedHeapBulkCopies() throws Exception {
        ByteBuffer b = ByteBuffer.allocate(8);
        b.put(0, (byte) 1);
        b.put(1, (byte) 2);
        b.put(2, (byte) 3);
        b.put(3, (byte) 4);
        b.put(4, (byte) 5);
        b.put(5, (byte) 6);
        b.put(6, (byte) 7);
        b.put(7, (byte) 8);

        char[] chars = new char[4];
        CharBuffer cb;
        cb = b.order(ByteOrder.BIG_ENDIAN).asCharBuffer();
        cb.get(chars);
        assertEquals(Arrays.toString(new char[] { '\u0102', '\u0304', '\u0506', '\u0708' }), Arrays.toString(chars));
        cb = b.order(ByteOrder.LITTLE_ENDIAN).asCharBuffer();
        cb.get(chars);
        assertEquals(Arrays.toString(new char[] { '\u0201', '\u0403', '\u0605', '\u0807' }), Arrays.toString(chars));

        double[] doubles = new double[1];
        DoubleBuffer db;
        db = b.order(ByteOrder.BIG_ENDIAN).asDoubleBuffer();
        db.get(doubles);
        assertEquals(0x0102030405060708L, Double.doubleToRawLongBits(doubles[0]));
        db = b.order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer();
        db.get(doubles);
        assertEquals(0x0807060504030201L, Double.doubleToRawLongBits(doubles[0]));

        float[] floats = new float[2];
        FloatBuffer fb;
        fb = b.order(ByteOrder.BIG_ENDIAN).asFloatBuffer();
        fb.get(floats);
        assertEquals(0x01020304, Float.floatToRawIntBits(floats[0]));
        assertEquals(0x05060708, Float.floatToRawIntBits(floats[1]));
        fb = b.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        fb.get(floats);
        assertEquals(0x04030201, Float.floatToRawIntBits(floats[0]));
        assertEquals(0x08070605, Float.floatToRawIntBits(floats[1]));

        int[] ints = new int[2];
        IntBuffer ib;
        ib = b.order(ByteOrder.BIG_ENDIAN).asIntBuffer();
        ib.get(ints);
        assertEquals(0x01020304, ints[0]);
        assertEquals(0x05060708, ints[1]);
        ib = b.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        ib.get(ints);
        assertEquals(0x04030201, ints[0]);
        assertEquals(0x08070605, ints[1]);

        long[] longs = new long[1];
        LongBuffer lb;
        lb = b.order(ByteOrder.BIG_ENDIAN).asLongBuffer();
        lb.get(longs);
        assertEquals(0x0102030405060708L, longs[0]);
        lb = b.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
        lb.get(longs);
        assertEquals(0x0807060504030201L, longs[0]);

        short[] shorts = new short[4];
        ShortBuffer sb;
        sb = b.order(ByteOrder.BIG_ENDIAN).asShortBuffer();
        sb.get(shorts);
        assertEquals(0x0102, shorts[0]);
        assertEquals(0x0304, shorts[1]);
        assertEquals(0x0506, shorts[2]);
        assertEquals(0x0708, shorts[3]);
        sb = b.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        sb.get(shorts);
        assertEquals(0x0201, shorts[0]);
        assertEquals(0x0403, shorts[1]);
        assertEquals(0x0605, shorts[2]);
        assertEquals(0x0807, shorts[3]);
    }
}
