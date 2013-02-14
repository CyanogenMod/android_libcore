/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package benchmarks.regression;

import java.nio.charset.Charset;
import com.google.caliper.Param;
import com.google.caliper.SimpleBenchmark;

public class CharsetBenchmark extends SimpleBenchmark {
    @Param({ "1", "10", "100", "1000", "10000" })
    private int length;

    // canonical    => canonical charset name
    // built-in     => guaranteed-present charset
    // special-case => libcore treats this charset specially for performance
    @Param({
        "UTF-16",     //     canonical,     built-in, non-special-case
        "UTF-8",      //     canonical,     built-in,     special-case
        "UTF8",       // non-canonical,     built-in,     special-case
        "ISO-8859-1", //     canonical,     built-in,     special-case
        "8859_1",     // non-canonical,     built-in,     special-case
        "ISO-8859-2", //     canonical, non-built-in, non-special-case
        "8859_2",     // non-canonical, non-built-in, non-special-case
        "US-ASCII",   //     canonical,     built-in,     special-case
        "ASCII"       // non-canonical,     built-in,     special-case
    })
    private String name;

    public void time_new_String_BString(int reps) throws Exception {
        byte[] bytes = makeBytes(makeString(length));
        for (int i = 0; i < reps; ++i) {
            new String(bytes, name);
        }
    }

    public void time_new_String_BII(int reps) throws Exception {
      byte[] bytes = makeBytes(makeString(length));
      for (int i = 0; i < reps; ++i) {
        new String(bytes, 0, bytes.length);
      }
    }

    public void time_new_String_BIIString(int reps) throws Exception {
      byte[] bytes = makeBytes(makeString(length));
      for (int i = 0; i < reps; ++i) {
        new String(bytes, 0, bytes.length, name);
      }
    }

    public void time_String_getBytes(int reps) throws Exception {
        String string = makeString(length);
        for (int i = 0; i < reps; ++i) {
            string.getBytes(name);
        }
    }

    // FIXME: benchmark this pure-java implementation for US-ASCII and ISO-8859-1 too!

    /**
     * Translates the given characters to US-ASCII or ISO-8859-1 bytes, using the fact that
     * Unicode code points between U+0000 and U+007f inclusive are identical to US-ASCII, while
     * U+0000 to U+00ff inclusive are identical to ISO-8859-1.
     */
    private static byte[] toDirectMappedBytes(char[] chars, int offset, int length, int maxValidChar) {
        byte[] result = new byte[length];
        int o = offset;
        for (int i = 0; i < length; ++i) {
            int ch = chars[o++];
            result[i] = (byte) ((ch <= maxValidChar) ? ch : '?');
        }
        return result;
    }

    private static byte[] toUtf8Bytes(char[] chars, int offset, int length) {
        UnsafeByteSequence result = new UnsafeByteSequence(length);
        toUtf8Bytes(chars, offset, length, result);
        return result.toByteArray();
    }

    private static void toUtf8Bytes(char[] chars, int offset, int length, UnsafeByteSequence out) {
        final int end = offset + length;
        for (int i = offset; i < end; ++i) {
            int ch = chars[i];
            if (ch < 0x80) {
                // One byte.
                out.write(ch);
            } else if (ch < 0x800) {
                // Two bytes.
                out.write((ch >> 6) | 0xc0);
                out.write((ch & 0x3f) | 0x80);
            } else if (ch >= Character.MIN_SURROGATE && ch <= Character.MAX_SURROGATE) {
                // A supplementary character.
                char high = (char) ch;
                char low = (i + 1 != end) ? chars[i + 1] : '\u0000';
                if (!Character.isSurrogatePair(high, low)) {
                    out.write('?');
                    continue;
                }
                // Now we know we have a *valid* surrogate pair, we can consume the low surrogate.
                ++i;
                ch = Character.toCodePoint(high, low);
                // Four bytes.
                out.write((ch >> 18) | 0xf0);
                out.write(((ch >> 12) & 0x3f) | 0x80);
                out.write(((ch >> 6) & 0x3f) | 0x80);
                out.write((ch & 0x3f) | 0x80);
            } else {
                // Three bytes.
                out.write((ch >> 12) | 0xe0);
                out.write(((ch >> 6) & 0x3f) | 0x80);
                out.write((ch & 0x3f) | 0x80);
            }
        }
    }

    private static String makeString(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            result.append('A' + (i % 26));
        }
        return result.toString();
    }

    private static byte[] makeBytes(String s) {
        try {
            return s.getBytes("US-ASCII");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
