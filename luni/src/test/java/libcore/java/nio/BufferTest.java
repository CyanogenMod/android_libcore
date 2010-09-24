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
    public void testByteSwappedHeapBulkGet() throws Exception {
        ByteBuffer b = ByteBuffer.allocate(10);
        for (int i = 0; i < b.limit(); ++i) {
            b.put(i, (byte) i);
        }
        b.position(1);

        char[] chars = new char[6];
        b.order(ByteOrder.BIG_ENDIAN).asCharBuffer().get(chars, 1, 4);
        assertEquals("[\u0000, \u0102, \u0304, \u0506, \u0708, \u0000]", Arrays.toString(chars));
        b.order(ByteOrder.LITTLE_ENDIAN).asCharBuffer().get(chars, 1, 4);
        assertEquals("[\u0000, \u0201, \u0403, \u0605, \u0807, \u0000]", Arrays.toString(chars));

        double[] doubles = new double[3];
        b.order(ByteOrder.BIG_ENDIAN).asDoubleBuffer().get(doubles, 1, 1);
        assertEquals(0, Double.doubleToRawLongBits(doubles[0]));
        assertEquals(0x0102030405060708L, Double.doubleToRawLongBits(doubles[1]));
        assertEquals(0, Double.doubleToRawLongBits(doubles[2]));
        b.order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer().get(doubles, 1, 1);
        assertEquals(0, Double.doubleToRawLongBits(doubles[0]));
        assertEquals(0x0807060504030201L, Double.doubleToRawLongBits(doubles[1]));
        assertEquals(0, Double.doubleToRawLongBits(doubles[2]));

        float[] floats = new float[4];
        b.order(ByteOrder.BIG_ENDIAN).asFloatBuffer().get(floats, 1, 2);
        assertEquals(0, Float.floatToRawIntBits(floats[0]));
        assertEquals(0x01020304, Float.floatToRawIntBits(floats[1]));
        assertEquals(0x05060708, Float.floatToRawIntBits(floats[2]));
        assertEquals(0, Float.floatToRawIntBits(floats[3]));
        b.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(floats, 1, 2);
        assertEquals(0, Float.floatToRawIntBits(floats[0]));
        assertEquals(0x04030201, Float.floatToRawIntBits(floats[1]));
        assertEquals(0x08070605, Float.floatToRawIntBits(floats[2]));
        assertEquals(0, Float.floatToRawIntBits(floats[3]));

        int[] ints = new int[4];
        b.order(ByteOrder.BIG_ENDIAN).asIntBuffer().get(ints, 1, 2);
        assertEquals(0, ints[0]);
        assertEquals(0x01020304, ints[1]);
        assertEquals(0x05060708, ints[2]);
        assertEquals(0, ints[3]);
        b.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(ints, 1, 2);
        assertEquals(0, ints[0]);
        assertEquals(0x04030201, ints[1]);
        assertEquals(0x08070605, ints[2]);
        assertEquals(0, ints[3]);

        long[] longs = new long[3];
        b.order(ByteOrder.BIG_ENDIAN).asLongBuffer().get(longs, 1, 1);
        assertEquals(0, longs[0]);
        assertEquals(0x0102030405060708L, longs[1]);
        assertEquals(0, longs[2]);
        b.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer().get(longs, 1, 1);
        assertEquals(0, longs[0]);
        assertEquals(0x0807060504030201L, longs[1]);
        assertEquals(0, longs[2]);

        short[] shorts = new short[6];
        b.order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(shorts, 1, 4);
        assertEquals(0, shorts[0]);
        assertEquals(0x0102, shorts[1]);
        assertEquals(0x0304, shorts[2]);
        assertEquals(0x0506, shorts[3]);
        assertEquals(0x0708, shorts[4]);
        assertEquals(0, shorts[5]);
        b.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts, 1, 4);
        assertEquals(0, shorts[0]);
        assertEquals(0x0201, shorts[1]);
        assertEquals(0x0403, shorts[2]);
        assertEquals(0x0605, shorts[3]);
        assertEquals(0x0807, shorts[4]);
        assertEquals(0, shorts[5]);
    }

    public static String toString(ByteBuffer b) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < b.limit(); ++i) {
            result.append(String.format("%02x", (int) b.get(i)));
        }
        return result.toString();
    }

    public void testByteSwappedHeapBulkPut() throws Exception {
        ByteBuffer b = ByteBuffer.allocate(10);
        b.position(1);

        char[] chars = new char[] { '\u2222', '\u0102', '\u0304', '\u0506', '\u0708', '\u2222' };
        b.order(ByteOrder.BIG_ENDIAN).asCharBuffer().put(chars, 1, 4);
        assertEquals("00010203040506070800", toString(b));
        b.order(ByteOrder.LITTLE_ENDIAN).asCharBuffer().put(chars, 1, 4);
        assertEquals("00020104030605080700", toString(b));

        double[] doubles = new double[] { 0, Double.longBitsToDouble(0x0102030405060708L), 0 };
        b.order(ByteOrder.BIG_ENDIAN).asDoubleBuffer().put(doubles, 1, 1);
        assertEquals("00010203040506070800", toString(b));
        b.order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer().put(doubles, 1, 1);
        assertEquals("00080706050403020100", toString(b));

        float[] floats = new float[] { 0, Float.intBitsToFloat(0x01020304),
                Float.intBitsToFloat(0x05060708), 0 };
        b.order(ByteOrder.BIG_ENDIAN).asFloatBuffer().put(floats, 1, 2);
        assertEquals("00010203040506070800", toString(b));
        b.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(floats, 1, 2);
        assertEquals("00040302010807060500", toString(b));

        int[] ints = new int[] { 0, 0x01020304, 0x05060708, 0 };
        b.order(ByteOrder.BIG_ENDIAN).asIntBuffer().put(ints, 1, 2);
        assertEquals("00010203040506070800", toString(b));
        b.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(ints, 1, 2);
        assertEquals("00040302010807060500", toString(b));

        long[] longs = new long[] { 0, 0x0102030405060708L, 0 };
        b.order(ByteOrder.BIG_ENDIAN).asLongBuffer().put(longs, 1, 1);
        assertEquals("00010203040506070800", toString(b));
        b.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer().put(longs, 1, 1);
        assertEquals("00080706050403020100", toString(b));

        short[] shorts = new short[] { 0, 0x0102, 0x0304, 0x0506, 0x0708, 0 };
        b.order(ByteOrder.BIG_ENDIAN).asShortBuffer().put(shorts, 1, 4);
        assertEquals("00010203040506070800", toString(b));
        b.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts, 1, 4);
        assertEquals("00020104030605080700", toString(b));
    }
}
