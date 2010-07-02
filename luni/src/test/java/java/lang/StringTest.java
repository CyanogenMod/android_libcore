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

package java.lang;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Arrays;
import junit.framework.TestCase;

public class StringTest extends TestCase {
    public void testIsEmpty() {
        assertTrue("".isEmpty());
        assertFalse("x".isEmpty());
    }

    // The evil decoder keeps hold of the CharBuffer it wrote to.
    private static final class EvilCharsetDecoder extends CharsetDecoder {
        private static char[] chars;
        public EvilCharsetDecoder(Charset cs) {
            super(cs, 1.0f, 1.0f);
        }
        protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
            chars = out.array();
            int inLength = in.remaining();
            for (int i = 0; i < inLength; ++i) {
                in.put((byte) 'X');
                out.put('Y');
            }
            return CoderResult.UNDERFLOW;
        }
        public static void corrupt() {
            for (int i = 0; i < chars.length; ++i) {
                chars[i] = '$';
            }
        }
    }

    // The evil encoder tries to write to the CharBuffer it was given to
    // read from.
    private static final class EvilCharsetEncoder extends CharsetEncoder {
        public EvilCharsetEncoder(Charset cs) {
            super(cs, 1.0f, 1.0f);
        }
        protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
            int inLength = in.remaining();
            for (int i = 0; i < inLength; ++i) {
                in.put('x');
                out.put((byte) 'y');
            }
            return CoderResult.UNDERFLOW;
        }
    }

    private static final Charset EVIL_CHARSET = new Charset("evil", null) {
        public boolean contains(Charset charset) { return false; }
        public CharsetEncoder newEncoder() { return new EvilCharsetEncoder(this); }
        public CharsetDecoder newDecoder() { return new EvilCharsetDecoder(this); }
    };

    public void testGetBytes_MaliciousCharset() {
        try {
            String s = "hi";
            // Check that our encoder can't write to the input CharBuffer
            // it was given.
            s.getBytes(EVIL_CHARSET);
            fail(); // We shouldn't have got here!
        } catch (ReadOnlyBufferException expected) {
            // We caught you trying to be naughty!
        }
    }

    public void testStringFromCharset() {
        Charset cs = Charset.forName("UTF-8");
        byte[] bytes = new byte[] {(byte) 'h', (byte) 'i'};
        assertEquals("hi", new String(bytes, cs));
    }

    public void testStringFromCharset_MaliciousCharset() {
        Charset cs = EVIL_CHARSET;
        byte[] bytes = new byte[] {(byte) 'h', (byte) 'i'};
        final String result = new String(bytes, cs);
        assertEquals("YY", result); // (Our decoder always outputs 'Y's.)
        // Check that even if the decoder messes with the output CharBuffer
        // after we've created a string from it, it doesn't affect the string.
        EvilCharsetDecoder.corrupt();
        assertEquals("YY", result);
    }

    public void test_getBytes_bad() throws Exception {
        // Check that we use '?' as the replacement byte for invalid characters.
        assertEquals("[97, 63, 98]", Arrays.toString("a\u0666b".getBytes("US-ASCII")));
        assertEquals("[97, 63, 98]", Arrays.toString("a\u0666b".getBytes(Charset.forName("US-ASCII"))));
    }

    public void test_getBytes_UTF_8() {
        // We have a fast path implementation of String.getBytes for UTF-8.
        Charset cs = Charset.forName("UTF-8");

        // Test the empty string.
        assertEquals("[]", Arrays.toString("".getBytes(cs)));

        // Test one-byte characters.
        assertEquals("[0]", Arrays.toString("\u0000".getBytes(cs)));
        assertEquals("[127]", Arrays.toString("\u007f".getBytes(cs)));
        assertEquals("[104, 105]", Arrays.toString("hi".getBytes(cs)));

        // Test two-byte characters.
        assertEquals("[-62, -128]", Arrays.toString("\u0080".getBytes(cs)));
        assertEquals("[-39, -90]", Arrays.toString("\u0666".getBytes(cs)));
        assertEquals("[-33, -65]", Arrays.toString("\u07ff".getBytes(cs)));
        assertEquals("[104, -39, -90, 105]", Arrays.toString("h\u0666i".getBytes(cs)));

        // Test three-byte characters.
        assertEquals("[-32, -96, -128]", Arrays.toString("\u0800".getBytes(cs)));
        assertEquals("[-31, -120, -76]", Arrays.toString("\u1234".getBytes(cs)));
        assertEquals("[-17, -65, -65]", Arrays.toString("\uffff".getBytes(cs)));
        assertEquals("[104, -31, -120, -76, 105]", Arrays.toString("h\u1234i".getBytes(cs)));

        // Test supplementary characters.
        // Minimum supplementary character: U+10000
        assertEquals("[-16, -112, -128, -128]", Arrays.toString("\ud800\udc00".getBytes(cs)));
        // Random supplementary character: U+10381 Ugaritic letter beta
        assertEquals("[-16, -112, -114, -127]", Arrays.toString("\ud800\udf81".getBytes(cs)));
        // Maximum supplementary character: U+10FFFF
        assertEquals("[-12, -113, -65, -65]", Arrays.toString("\udbff\udfff".getBytes(cs)));
        // A high surrogate at end of string is an error replaced with '?'.
        assertEquals("[104, 63]", Arrays.toString("h\ud800".getBytes(cs)));
        // A high surrogate not followed by a low surrogate is an error replaced with '?'.
        assertEquals("[104, 63, 105]", Arrays.toString("h\ud800i".getBytes(cs)));
    }

    public void test_new_String_bad() throws Exception {
        // Check that we use U+FFFD as the replacement string for invalid bytes.
        assertEquals("a\ufffdb", new String(new byte[] { 97, -2, 98 }, "US-ASCII"));
        assertEquals("a\ufffdb", new String(new byte[] { 97, -2, 98 }, Charset.forName("US-ASCII")));
    }
}
