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

package java.net;

import com.ibm.icu.text.IDNA;

/**
 * Converts internationalized domain names between Unicode and the ASCII Compatible Encoding
 * (ACE) representation.
 *
 * <p>See <a href="http://www.ietf.org/rfc/rfc3490.txt">RFC 3490</a> for full details.
 *
 * @since 1.6
 */
public final class IDN {
    /**
     * When set, allows IDN to process unassigned unicode points.
     * No longer supported under UTS46.
     */
    public static final int ALLOW_UNASSIGNED = IDNA.ALLOW_UNASSIGNED;

    /**
     * When set, ASCII strings are checked against
     * <a href="http://www.ietf.org/rfc/rfc1122.txt">RFC 1122</a> and
     * <a href="http://www.ietf.org/rfc/rfc1123.txt">RFC 1123</a>.
     */
    public static final int USE_STD3_ASCII_RULES = IDNA.USE_STD3_RULES;

    /**
     * Check whether the input conforms to BiDi rules.
     * @hide
     */
    public static final int CHECK_BIDI = IDNA.CHECK_BIDI;

    /**
     * Check whether the input conforms to CONTEXTJ rules.
     * @hide
     */
    public static final int CHECK_CONTEXTJ = IDNA.CHECK_CONTEXTJ;

    /**
     * Check whether the input conforms to CONTEXTO rules,
     * @hide
     */
    public static final int CHECK_CONTEXTO = IDNA.CHECK_CONTEXTO;

    private IDN() {
    }

    /**
     * Transform a Unicode String to ASCII Compatible Encoding String according
     * to the algorithm defined in <a href="http://www.ietf.org/rfc/rfc3490.txt">RFC 3490</a>.
     *
     * <p>If the transformation fails (because the input is not a valid IDN), an
     * exception will be thrown.
     *
     * <p>This method can handle either an individual label or an entire domain name.
     * In the latter case, the separators are: U+002E (full stop), U+3002 (ideographic full stop),
     * U+FF0E (fullwidth full stop), and U+FF61 (halfwidth ideographic full stop).
     * All of these will become U+002E (full stop) in the result.
     *
     * @param input the Unicode name
     * @param flags Define the rules for parsing internationalized domain names.
     *              This can take the values of the constants defined in this class.
     * @return the ACE name
     * @throws IllegalArgumentException if {@code input} does not conform to <a href="http://www.ietf.org/rfc/rfc3490.txt">RFC 3490</a>
     */
    public static String toASCII(String input, int flags) {
        IDNA converter = IDNA.getUTS46Instance(flags);
        StringBuilder result = new StringBuilder();
        IDNA.Info info = new IDNA.Info();
        converter.nameToASCII(input, result, info);

        if (info.hasErrors()) {
            throw new IllegalArgumentException("Invalid input to toASCII: " + input);
        }

        return result.toString();
    }

    /**
     * Equivalent to {@code toASCII(input, 0)}.
     *
     * @param input the Unicode name
     * @return the ACE name
     * @throws IllegalArgumentException if {@code input} does not conform to <a href="http://www.ietf.org/rfc/rfc3490.txt">RFC 3490</a>
     */
    public static String toASCII(String input) {
        return toASCII(input, 0);
    }

    /**
     * Translates a string from ASCII Compatible Encoding (ACE) to Unicode
     * according to the algorithm defined in <a href="http://www.ietf.org/rfc/rfc3490.txt">RFC 3490</a>.
     *
     * <p>Unlike {@code toASCII}, this transformation cannot fail.
     *
     * <p>This method can handle either an individual label or an entire domain name.
     * In the latter case, the separators are: U+002E (full stop), U+3002 (ideographic full stop),
     * U+FF0E (fullwidth full stop), and U+FF61 (halfwidth ideographic full stop).
     *
     * @param input the ACE name
     * @return the Unicode name
     * @param flags Define the rules for parsing internationalized domain names.
     *              This can take the values of the constants defined in this class.
     */
    public static String toUnicode(String input, int flags) {
        IDNA converter = IDNA.getUTS46Instance(flags);
        StringBuilder result = new StringBuilder();
        IDNA.Info info = new IDNA.Info();
        converter.nameToUnicode(input, result, info);

        // To maintain compatibility, if there was an error parsing, the original string
        // is returned.
        if (info.hasErrors()) {
            return input;
        }

        return result.toString();
    }

    /**
     * Equivalent to {@code toUnicode(input, 0)}.
     *
     * @param input the ACE name
     * @return the Unicode name
     */
    public static String toUnicode(String input) {
        return toUnicode(input, 0);
    }
}
