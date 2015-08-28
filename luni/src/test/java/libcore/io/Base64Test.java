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

package libcore.io;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import junit.framework.TestCase;

public final class Base64Test extends TestCase {

    public void testEncodeDecode() throws Exception {
        assertEncodeDecode("");
        assertEncodeDecode("Eg==", 0x12);
        assertEncodeDecode("EjQ=", 0x12, 0x34);
        assertEncodeDecode("EjRW", 0x12, 0x34, 0x56);
        assertEncodeDecode("EjRWeA==", 0x12, 0x34, 0x56, 0x78);
        assertEncodeDecode("EjRWeJo=", 0x12, 0x34, 0x56, 0x78, 0x9A);
        assertEncodeDecode("EjRWeJq8", 0x12, 0x34, 0x56, 0x78, 0x9a, 0xbc);
    }

    public void testEncode_doesNotWrap() {
        int[] data = new int[61];
        Arrays.fill(data, 0xff);
        String expected = "///////////////////////////////////////////////////////////////////////"
                + "//////////w=="; // 84 chars
        assertEncodeDecode(expected, data);
    }

    private static void assertEncodeDecode(String expectedEncoded, int... data) {
        byte[] inputBytes = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            inputBytes[i] = (byte) data[i];
        }
        String encoded = Base64.encode(inputBytes);
        assertEquals(expectedEncoded, encoded);

        byte[] actualDecodedBytes = Base64.decode(encoded.getBytes(StandardCharsets.US_ASCII));
        assertArrayEquals(inputBytes, actualDecodedBytes);
    }

    public void testDecode_empty() throws Exception {
        byte[] decoded = Base64.decode(new byte[0]);
        assertArrayEquals(new byte[0], decoded);
    }

    public void testDecode_truncated() throws Exception {
        // Correct data, for reference.
        assertEquals("hello, world", decodeToString("aGVsbG8sIHdvcmxk"));

        // The following are missing the final bytes
        assertEquals("hello, wo", decodeToString("aGVsbG8sIHdvcmx"));
        assertEquals("hello, wo", decodeToString("aGVsbG8sIHdvcm"));
        assertEquals("hello, wo", decodeToString("aGVsbG8sIHdvc"));
        assertEquals("hello, wo", decodeToString("aGVsbG8sIHdv"));
    }

    public void testDecode_extraChars() throws Exception {
        // Characters outside of alphabet before padding.
        assertEquals("hello, world", decodeToString(" aGVsbG8sIHdvcmxk"));
        assertEquals("hello, world", decodeToString("aGV sbG8sIHdvcmxk"));
        assertEquals("hello, world", decodeToString("aGVsbG8sIHdvcmxk "));
        assertEquals(null, decodeToString("*aGVsbG8sIHdvcmxk"));
        assertEquals(null, decodeToString("aGV*sbG8sIHdvcmxk"));
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmxk*"));
        assertEquals("hello, world", decodeToString("\r\naGVsbG8sIHdvcmxk"));
        assertEquals("hello, world", decodeToString("aGV\r\nsbG8sIHdvcmxk"));
        assertEquals("hello, world", decodeToString("aGVsbG8sIHdvcmxk\r\n"));
        assertEquals("hello, world", decodeToString("\naGVsbG8sIHdvcmxk"));
        assertEquals("hello, world", decodeToString("aGV\nsbG8sIHdvcmxk"));
        assertEquals("hello, world", decodeToString("aGVsbG8sIHdvcmxk\n"));

        // padding 0
        assertEquals("hello, world", decodeToString("aGVsbG8sIHdvcmxk"));
        // Extra padding
        assertDecodeBad("aGVsbG8sIHdvcmxk=");
        assertDecodeBad("aGVsbG8sIHdvcmxk==");
        // Characters outside alphabet intermixed with (too much) padding.
        assertDecodeBad("aGVsbG8sIHdvcmxk =");
        assertEquals("hello, world�", decodeToString("aGVsbG8sIHdvcmxk = = "));

        // padding 1
        assertEquals("hello, world?!", decodeToString("aGVsbG8sIHdvcmxkPyE="));
        // Missing padding
        assertEquals("hello, world", decodeToString("aGVsbG8sIHdvcmxkPyE"));
        // Characters outside alphabet before padding.
        assertEquals("hello, world?!", decodeToString("aGVsbG8sIHdvcmxkPyE ="));
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmxkPyE*="));
        // Trailing characters, otherwise valid.
        assertEquals("hello, world?!", decodeToString("aGVsbG8sIHdvcmxkPyE= "));
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmxkPyE=*"));
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmxkPyE=X"));
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmxkPyE=XY"));
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmxkPyE=XYZ"));
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmxkPyE=XYZA"));
        assertEquals("hello, world?!", decodeToString("aGVsbG8sIHdvcmxkPyE=\n"));
        assertEquals("hello, world?!", decodeToString("aGVsbG8sIHdvcmxkPyE=\r\n"));
        assertEquals("hello, world?!", decodeToString("aGVsbG8sIHdvcmxkPyE= "));
        assertEquals("hello, world�", decodeToString("aGVsbG8sIHdvcmxkPyE=="));
        // Characters outside alphabet intermixed with (too much) padding.
        assertEquals("hello, world�", decodeToString("aGVsbG8sIHdvcmxkPyE =="));
        assertEquals("hello, world�", decodeToString("aGVsbG8sIHdvcmxkPyE = = "));

        // padding 2
        assertEquals("hello, world.", decodeToString("aGVsbG8sIHdvcmxkLg=="));
        // Missing padding
        assertEquals("hello, world", decodeToString("aGVsbG8sIHdvcmxkLg"));
        // Partially missing padding
        assertDecodeBad("aGVsbG8sIHdvcmxkLg=");
        // Characters outside alphabet before padding.
        assertEquals("hello, world.", decodeToString("aGVsbG8sIHdvcmxkLg =="));
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmxkLg*=="));
        // Trailing characters, otherwise valid.
        assertEquals("hello, world.", decodeToString("aGVsbG8sIHdvcmxkLg== "));
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmxkLg==*"));
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmxkLg==X"));
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmxkLg==XY"));
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmxkLg==XYZ"));
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmxkLg==XYZA"));
        assertEquals("hello, world.", decodeToString("aGVsbG8sIHdvcmxkLg==\n"));
        assertEquals("hello, world.", decodeToString("aGVsbG8sIHdvcmxkLg==\r\n"));
        assertEquals("hello, world.", decodeToString("aGVsbG8sIHdvcmxkLg== "));
        assertEquals("hello, world�", decodeToString("aGVsbG8sIHdvcmxkLg==="));
        // Characters outside alphabet inside padding.
        assertEquals("hello, world.", decodeToString("aGVsbG8sIHdvcmxkLg= ="));
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmxkLg=*="));
        assertEquals("hello, world.", decodeToString("aGVsbG8sIHdvcmxkLg=\r\n="));
        // Characters inside alphabet inside padding.
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmxkLg=X="));

        // Table 1 chars
        assertEquals(null, decodeToString("_aGVsbG8sIHdvcmx"));
        assertEquals(null, decodeToString("aGV_sbG8sIHdvcmx"));
        assertEquals(null, decodeToString("aGVsbG8sIHdvcmx_"));

        // Table 2 chars.
        assertEquals("������������", decodeToString("/aGVsbG8sIHdvcmx"));
        assertEquals("he���������", decodeToString("aGV/sbG8sIHdvcmx"));
        assertEquals("hello, worl\u007F", decodeToString("aGVsbG8sIHdvcmx/"));
    }

    private static final byte[] BYTES = { (byte) 0xff, (byte) 0xee, (byte) 0xdd,
        (byte) 0xcc, (byte) 0xbb, (byte) 0xaa,
        (byte) 0x99, (byte) 0x88, (byte) 0x77 };

    public void testDecode_nonPrintableBytes() throws Exception {
        assertSubArrayEquals(BYTES, 0, decodeToBytes(""));
        assertSubArrayEquals(BYTES, 1, decodeToBytes("/w=="));
        assertSubArrayEquals(BYTES, 2, decodeToBytes("/+4="));
        assertSubArrayEquals(BYTES, 3, decodeToBytes("/+7d"));
        assertSubArrayEquals(BYTES, 4, decodeToBytes("/+7dzA=="));
        assertSubArrayEquals(BYTES, 5, decodeToBytes("/+7dzLs="));
        assertSubArrayEquals(BYTES, 6, decodeToBytes("/+7dzLuq"));
        assertSubArrayEquals(BYTES, 7, decodeToBytes("/+7dzLuqmQ=="));
        assertSubArrayEquals(BYTES, 8, decodeToBytes("/+7dzLuqmYg="));
    }

    public void testDecode_nonPrintableBytes_urlAlphabet() throws Exception {
        assertNull(decodeToBytes("_w=="));
        assertNull(decodeToBytes("_-4="));
        assertNull(decodeToBytes("_-7d"));
        assertNull(decodeToBytes("_-7dzA=="));
        assertNull(decodeToBytes("_-7dzLs="));
        assertNull(decodeToBytes("_-7dzLuq"));
        assertNull(decodeToBytes("_-7dzLuqmQ=="));
        assertNull(decodeToBytes("_-7dzLuqmYg="));
    }

    /** Decodes a string, returning a string or null. */
    private static String decodeToString(String in) throws Exception {
        byte[] out = decodeToBytes(in);
        return out == null ? null : new String(out, StandardCharsets.US_ASCII);
    }

    /** Decodes a string, returning a string. */
    private static byte[] decodeToBytes(String in) throws Exception {
        return Base64.decode(in.getBytes(StandardCharsets.US_ASCII));
    }

    /** Assert that decoding 'in' throws ArrayIndexOutOfBoundsException. */
    private static void assertDecodeBad(String in) throws Exception {
        try {
            byte[] result = Base64.decode(in.getBytes(StandardCharsets.US_ASCII));
            fail("should have failed to decode. Actually: " +
                (result == null ? result : new String(result, StandardCharsets.US_ASCII)));
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    private static void assertArrayEquals(byte[] expected, byte[] actual) {
        assertSubArrayEquals(expected, expected.length, actual);
    }

    /** Assert that actual equals the first len bytes of expected. */
    private static void assertSubArrayEquals(byte[] expected, int len, byte[] actual) {
        assertEquals(len, actual.length);
        for (int i = 0; i < len; ++i) {
            assertEquals(expected[i], actual[i]);
        }
    }
}

