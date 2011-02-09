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

package libcore.net;

import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.Charsets;

/**
 * Encodes and decodes {@code application/x-www-form-urlencoded} content.
 * Subclasses define exactly which characters are legal.
 *
 * <p>By default, UTF-8 is used to encode escaped characters. A single input
 * character like "\u0080" may be encoded to multiple octets like %C2%80.
 */
public abstract class UriCodec {

    /**
     * Returns true if {@code c} does not need to be escaped.
     */
    protected abstract boolean isRetained(char c);

    /**
     * Throws if {@code s} is invalid according to this encoder.
     */
    public void validate(String s) throws URISyntaxException {
        for (int i = 0; i < s.length();) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || isRetained(ch)) {
                i++;
            } else if (ch == '%') {
                if (i + 2 >= s.length()) {
                    throw new URISyntaxException(s, "Incomplete % sequence", i);
                }
                int d1 = Character.digit(s.charAt(i + 1), 16);
                int d2 = Character.digit(s.charAt(i + 2), 16);
                if (d1 == -1 || d2 == -1) {
                    throw new URISyntaxException(s, "Invalid % sequence: " +
                            s.substring(i, i + 3), i);
                }
                i += 3;
            } else {
                throw new URISyntaxException(s, "Illegal character", i);
            }
        }
    }

    /**
     * Throws if {@code s} contains characters that are not letters, digits or
     * in {@code legal}.
     */
    public static void validateSimple(String s, String legal) throws URISyntaxException {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (!((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || legal.indexOf(ch) > -1)) {
                throw new URISyntaxException(s, "Illegal character", i);
            }
        }
    }

    /**
     * Encodes {@code s} and appends the result to {@code builder}.
     */
    public void appendEncoded(StringBuilder builder, String s, Charset charset) {
        if (s == null) {
            throw new NullPointerException();
        }

        int escapeStart = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || isRetained(c)) {
                if (escapeStart != -1) {
                    appendHex(builder, s.substring(escapeStart, i), charset);
                    escapeStart = -1;
                }
                if (c != ' ') {
                    builder.append(c);
                } else {
                    builder.append('+');
                }
            } else if (escapeStart == -1) {
                escapeStart = i;
            }
        }
        if (escapeStart != -1) {
            appendHex(builder, s.substring(escapeStart, s.length()), charset);
        }
    }

    /**
     * Encodes {@code s} and appends the result to {@code builder}.
     */
    public void appendEncoded(StringBuilder builder, String s) {
        appendEncoded(builder, s, Charsets.UTF_8);
    }

    /**
     * Unlike the methods in {@code URI}, this method ignores input that has
     * already been escaped. For example, input of "hello%20world" is unchanged
     * by this method but would be double-escaped to "hello%2520world" by URI.
     */
    public String fixEncoding(String s) {
        StringBuilder result = null;
        int copiedCount = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '%') {
                i += 2; // this is a 3-character sequence like "%20"
                continue;
            }

            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || isRetained(c)) {
                continue;
            }

            /*
             * We've encountered a character that must be escaped.
             */
            if (result == null) {
                result = new StringBuilder();
            }
            result.append(s, copiedCount, i);

            if (c < 0x7f) {
                appendHex(result, (byte) c);
            } else {
                for (byte b : s.substring(i, i + 1).getBytes(Charsets.UTF_8)) {
                    appendHex(result, b);
                }
            }

            copiedCount = i + 1;
        }

        if (result == null) {
            return s;
        } else {
            result.append(s, copiedCount, s.length());
            return result.toString();
        }
    }

    /**
     * @param convertPlus true to convert '+' to ' '.
     */
    public static String decode(String s, boolean convertPlus, Charset charset) {
        if (s.indexOf('%') == -1 && (!convertPlus || s.indexOf('+') == -1)) {
            return s;
        }

        StringBuilder result = new StringBuilder(s.length());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < s.length();) {
            char c = s.charAt(i);
            if (c == '%') {
                do {
                    if (i + 2 >= s.length()) {
                        throw new IllegalArgumentException("Incomplete % sequence at: " + i);
                    }
                    int d1 = Character.digit(s.charAt(i + 1), 16);
                    int d2 = Character.digit(s.charAt(i + 2), 16);
                    if (d1 == -1 || d2 == -1) {
                        throw new IllegalArgumentException("Invalid % sequence " +
                                s.substring(i, i + 3) + " at " + i);
                    }
                    out.write((byte) ((d1 << 4) + d2));
                    i += 3;
                } while (i < s.length() && s.charAt(i) == '%');
                result.append(new String(out.toByteArray(), charset));
                out.reset();
            } else {
                if (convertPlus && c == '+') {
                    c = ' ';
                }
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    public static String decode(String s) {
        return decode(s, false, Charsets.UTF_8);
    }

    private static void appendHex(StringBuilder builder, String s, Charset charset) {
        for (byte b : s.getBytes(charset)) {
            appendHex(builder, b);
        }
    }

    private static void appendHex(StringBuilder sb, byte b) {
        sb.append('%');
        sb.append(Byte.toHexString(b, true));
    }
}
