/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
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

package java.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

import com.ibm.icu.math.MathContext;
import libcore.icu.LocaleData;

/**
 * A concrete subclass of {@link NumberFormat} that formats decimal numbers. It
 * has a variety of features designed to make it possible to parse and format
 * numbers in any locale, including support for Western, Arabic, or Indic
 * digits. It also supports different flavors of numbers, including integers
 * ("123"), fixed-point numbers ("123.4"), scientific notation ("1.23E4"),
 * percentages ("12%"), and currency amounts ("$123"). All of these flavors can
 * be easily localized.
 * <p>
 * <strong>This is an enhanced version of {@code DecimalFormat} that is based on
 * the standard version in the RI. New or changed functionality is labeled
 * <strong><font color="red">NEW</font></strong>.</strong>
 * <p>
 * To obtain a {@link NumberFormat} for a specific locale (including the default
 * locale), call one of {@code NumberFormat}'s factory methods such as
 * {@code NumberFormat.getInstance}. Do not call the {@code DecimalFormat}
 * constructors directly, unless you know what you are doing, since the
 * {@link NumberFormat} factory methods may return subclasses other than
 * {@code DecimalFormat}. If you need to customize the format object, do
 * something like this: <blockquote>
 *
 * <pre>
 * NumberFormat f = NumberFormat.getInstance(loc);
 * if (f instanceof DecimalFormat) {
 *     ((DecimalFormat)f).setDecimalSeparatorAlwaysShown(true);
 * }
 * </pre>
 *
 * </blockquote>
 *
 * <h4>Patterns</h4>
 * <p>
 * A {@code DecimalFormat} consists of a <em>pattern</em> and a set of
 * <em>symbols</em>. The pattern may be set directly using
 * {@link #applyPattern(String)}, or indirectly using other API methods which
 * manipulate aspects of the pattern, such as the minimum number of integer
 * digits. The symbols are stored in a {@link DecimalFormatSymbols} object. When
 * using the {@link NumberFormat} factory methods, the pattern and symbols are
 * read from ICU's locale data.
 * <h4>Special Pattern Characters</h4>
 * <p>
 * Many characters in a pattern are taken literally; they are matched during
 * parsing and are written out unchanged during formatting. On the other hand,
 * special characters stand for other characters, strings, or classes of
 * characters. For example, the '#' character is replaced by a localized digit.
 * Often the replacement character is the same as the pattern character; in the
 * U.S. locale, the ',' grouping character is replaced by ','. However, the
 * replacement is still happening, and if the symbols are modified, the grouping
 * character changes. Some special characters affect the behavior of the
 * formatter by their presence; for example, if the percent character is seen,
 * then the value is multiplied by 100 before being displayed.
 * <p>
 * To insert a special character in a pattern as a literal, that is, without any
 * special meaning, the character must be quoted. There are some exceptions to
 * this which are noted below.
 * <p>
 * The characters listed here are used in non-localized patterns. Localized
 * patterns use the corresponding characters taken from this formatter's
 * {@link DecimalFormatSymbols} object instead, and these characters lose their
 * special status. Two exceptions are the currency sign and quote, which are not
 * localized.
 * <blockquote> <table border="0" cellspacing="3" cellpadding="0" summary="Chart
 * showing symbol, location, localized, and meaning.">
 * <tr bgcolor="#ccccff">
 * <th align="left">Symbol</th>
 * <th align="left">Location</th>
 * <th align="left">Localized?</th>
 * <th align="left">Meaning</th>
 * </tr>
 * <tr valign="top">
 * <td>{@code 0}</td>
 * <td>Number</td>
 * <td>Yes</td>
 * <td>Digit.</td>
 * </tr>
 * <tr valign="top">
 * <td>{@code @}</td>
 * <td>Number</td>
 * <td>No</td>
 * <td><strong><font color="red">NEW</font>&nbsp;</strong> Significant
 * digit.</td>
 * </tr>
 * <tr valign="top" bgcolor="#eeeeff">
 * <td>{@code #}</td>
 * <td>Number</td>
 * <td>Yes</td>
 * <td>Digit, leading zeroes are not shown.</td>
 * </tr>
 * <tr valign="top">
 * <td>{@code .}</td>
 * <td>Number</td>
 * <td>Yes</td>
 * <td>Decimal separator or monetary decimal separator.</td>
 * </tr>
 * <tr valign="top" bgcolor="#eeeeff">
 * <td>{@code -}</td>
 * <td>Number</td>
 * <td>Yes</td>
 * <td>Minus sign.</td>
 * </tr>
 * <tr valign="top">
 * <td>{@code ,}</td>
 * <td>Number</td>
 * <td>Yes</td>
 * <td>Grouping separator.</td>
 * </tr>
 * <tr valign="top" bgcolor="#eeeeff">
 * <td>{@code E}</td>
 * <td>Number</td>
 * <td>Yes</td>
 * <td>Separates mantissa and exponent in scientific notation.
 * <em>Does not need to be quoted in prefix or suffix.</em></td>
 * </tr>
 * <tr valign="top">
 * <td>{@code +}</td>
 * <td>Exponent</td>
 * <td>Yes</td>
 * <td><strong><font color="red">NEW</font>&nbsp;</strong> Prefix
 * positive exponents with localized plus sign.
 * <em>Does not need to be quoted in prefix or suffix.</em></td>
 * </tr>
 * <tr valign="top" bgcolor="#eeeeff">
 * <td>{@code ;}</td>
 * <td>Subpattern boundary</td>
 * <td>Yes</td>
 * <td>Separates positive and negative subpatterns.</td>
 * </tr>
 * <tr valign="top">
 * <td>{@code %}</td>
 * <td>Prefix or suffix</td>
 * <td>Yes</td>
 * <td>Multiply by 100 and show as percentage.</td>
 * </tr>
 * <tr valign="top" bgcolor="#eeeeff">
 * <td>{@code \u2030} ({@code \u005Cu2030})</td>
 * <td>Prefix or suffix</td>
 * <td>Yes</td>
 * <td>Multiply by 1000 and show as per mille.</td>
 * </tr>
 * <tr valign="top">
 * <td>{@code \u00A4} ({@code \u005Cu00A4})</td>
 * <td>Prefix or suffix</td>
 * <td>No</td>
 * <td>Currency sign, replaced by currency symbol. If doubled, replaced by
 * international currency symbol. If present in a pattern, the monetary decimal
 * separator is used instead of the decimal separator.</td>
 * </tr>
 * <tr valign="top" bgcolor="#eeeeff">
 * <td>{@code '}</td>
 * <td>Prefix or suffix</td>
 * <td>No</td>
 * <td>Used to quote special characters in a prefix or suffix, for example,
 * {@code "'#'#"} formats 123 to {@code "#123"}. To create a single quote
 * itself, use two in a row: {@code "# o''clock"}.</td>
 * </tr>
 * <tr valign="top">
 * <td>{@code *}</td>
 * <td>Prefix or suffix boundary</td>
 * <td>Yes</td>
 * <td><strong><font color="red">NEW</font>&nbsp;</strong> Pad escape,
 * precedes pad character. </td>
 * </tr>
 * </table> </blockquote>
 * <p>
 * A {@code DecimalFormat} pattern contains a positive and negative subpattern,
 * for example, "#,##0.00;(#,##0.00)". Each subpattern has a prefix, a numeric
 * part and a suffix. If there is no explicit negative subpattern, the negative
 * subpattern is the localized minus sign prefixed to the positive subpattern.
 * That is, "0.00" alone is equivalent to "0.00;-0.00". If there is an explicit
 * negative subpattern, it serves only to specify the negative prefix and
 * suffix; the number of digits, minimal digits, and other characteristics are
 * ignored in the negative subpattern. This means that "#,##0.0#;(#)" produces
 * precisely the same result as "#,##0.0#;(#,##0.0#)".
 * <p>
 * The prefixes, suffixes, and various symbols used for infinity, digits,
 * thousands separators, decimal separators, etc. may be set to arbitrary
 * values, and they will appear properly during formatting. However, care must
 * be taken that the symbols and strings do not conflict, or parsing will be
 * unreliable. For example, either the positive and negative prefixes or the
 * suffixes must be distinct for {@link #parse} to be able to distinguish
 * positive from negative values. Another example is that the decimal separator
 * and thousands separator should be distinct characters, or parsing will be
 * impossible.
 * <p>
 * The <em>grouping separator</em> is a character that separates clusters of
 * integer digits to make large numbers more legible. It is commonly used for
 * thousands, but in some locales it separates ten-thousands. The <em>grouping
 * size</em>
 * is the number of digits between the grouping separators, such as 3 for
 * "100,000,000" or 4 for "1 0000 0000". There are actually two different
 * grouping sizes: One used for the least significant integer digits, the
 * <em>primary grouping size</em>, and one used for all others, the
 * <em>secondary grouping size</em>. In most locales these are the same, but
 * sometimes they are different. For example, if the primary grouping interval
 * is 3, and the secondary is 2, then this corresponds to the pattern
 * "#,##,##0", and the number 123456789 is formatted as "12,34,56,789". If a
 * pattern contains multiple grouping separators, the interval between the last
 * one and the end of the integer defines the primary grouping size, and the
 * interval between the last two defines the secondary grouping size. All others
 * are ignored, so "#,##,###,####", "###,###,####" and "##,#,###,####" produce
 * the same result.
 * <p>
 * Illegal patterns, such as "#.#.#" or "#.###,###", will cause
 * {@code DecimalFormat} to throw an {@link IllegalArgumentException} with a
 * message that describes the problem.
 * <h4>Pattern BNF</h4>
 *
 * <pre>
 * pattern    := subpattern (';' subpattern)?
 * subpattern := prefix? number exponent? suffix?
 * number     := (integer ('.' fraction)?) | sigDigits
 * prefix     := '\\u0000'..'\\uFFFD' - specialCharacters
 * suffix     := '\\u0000'..'\\uFFFD' - specialCharacters
 * integer    := '#'* '0'* '0'
 * fraction   := '0'* '#'*
 * sigDigits  := '#'* '@' '@'* '#'*
 * exponent   := 'E' '+'? '0'* '0'
 * padSpec    := '*' padChar
 * padChar    := '\\u0000'..'\\uFFFD' - quote
 *
 * Notation:
 *   X*       0 or more instances of X
 *   X?       0 or 1 instances of X
 *   X|Y      either X or Y
 *   C..D     any character from C up to D, inclusive
 *   S-T      characters in S, except those in T
 * </pre>
 *
 * The first subpattern is for positive numbers. The second (optional)
 * subpattern is for negative numbers.
 * <p>
 * Not indicated in the BNF syntax above:
 * <ul>
 * <li>The grouping separator ',' can occur inside the integer and sigDigits
 * elements, between any two pattern characters of that element, as long as the
 * integer or sigDigits element is not followed by the exponent element.
 * <li><font color="red"><strong>NEW</strong>&nbsp;</font> Two
 * grouping intervals are recognized: The one between the decimal point and the
 * first grouping symbol and the one between the first and second grouping
 * symbols. These intervals are identical in most locales, but in some locales
 * they differ. For example, the pattern &quot;#,##,###&quot; formats the number
 * 123456789 as &quot;12,34,56,789&quot;.</li>
 * <li> <strong><font color="red">NEW</font>&nbsp;</strong> The pad
 * specifier {@code padSpec} may appear before the prefix, after the prefix,
 * before the suffix, after the suffix or not at all.
 * </ul>
 * <h4>Parsing</h4>
 * <p>
 * {@code DecimalFormat} parses all Unicode characters that represent decimal
 * digits, as defined by {@link Character#digit(int, int)}. In addition,
 * {@code DecimalFormat} also recognizes as digits the ten consecutive
 * characters starting with the localized zero digit defined in the
 * {@link DecimalFormatSymbols} object. During formatting, the
 * {@link DecimalFormatSymbols}-based digits are written out.
 * <p>
 * During parsing, grouping separators are ignored.
 * <p>
 * If {@link #parse(String, ParsePosition)} fails to parse a string, it returns
 * {@code null} and leaves the parse position unchanged.
 * <h4>Formatting</h4>
 * <p>
 * Formatting is guided by several parameters, all of which can be specified
 * either using a pattern or using the API. The following description applies to
 * formats that do not use <a href="#sci">scientific notation</a> or <a
 * href="#sigdig">significant digits</a>.
 * <ul>
 * <li>If the number of actual integer digits exceeds the
 * <em>maximum integer digits</em>, then only the least significant digits
 * are shown. For example, 1997 is formatted as "97" if maximum integer digits
 * is set to 2.
 * <li>If the number of actual integer digits is less than the
 * <em>minimum integer digits</em>, then leading zeros are added. For
 * example, 1997 is formatted as "01997" if minimum integer digits is set to 5.
 * <li>If the number of actual fraction digits exceeds the <em>maximum
 * fraction digits</em>,
 * then half-even rounding is performed to the maximum fraction digits. For
 * example, 0.125 is formatted as "0.12" if the maximum fraction digits is 2.
 * <li>If the number of actual fraction digits is less than the
 * <em>minimum fraction digits</em>, then trailing zeros are added. For
 * example, 0.125 is formatted as "0.1250" if the minimum fraction digits is set
 * to 4.
 * <li>Trailing fractional zeros are not displayed if they occur <em>j</em>
 * positions after the decimal, where <em>j</em> is less than the maximum
 * fraction digits. For example, 0.10004 is formatted as "0.1" if the maximum
 * fraction digits is four or less.
 * </ul>
 * <p>
 * <strong>Special Values</strong>
 * <p>
 * {@code NaN} is represented as a single character, typically
 * {@code \u005cuFFFD}. This character is determined by the
 * {@link DecimalFormatSymbols} object. This is the only value for which the
 * prefixes and suffixes are not used.
 * <p>
 * Infinity is represented as a single character, typically {@code \u005cu221E},
 * with the positive or negative prefixes and suffixes applied. The infinity
 * character is determined by the {@link DecimalFormatSymbols} object. <a
 * name="sci">
 * <h4>Scientific Notation</h4>
 * </a>
 * <p>
 * Numbers in scientific notation are expressed as the product of a mantissa and
 * a power of ten, for example, 1234 can be expressed as 1.234 x 10<sup>3</sup>.
 * The mantissa is typically in the half-open interval [1.0, 10.0) or sometimes
 * [0.0, 1.0), but it does not need to be. {@code DecimalFormat} supports
 * arbitrary mantissas. {@code DecimalFormat} can be instructed to use
 * scientific notation through the API or through the pattern. In a pattern, the
 * exponent character immediately followed by one or more digit characters
 * indicates scientific notation. Example: "0.###E0" formats the number 1234 as
 * "1.234E3".
 * <ul>
 * <li>The number of digit characters after the exponent character gives the
 * minimum exponent digit count. There is no maximum. Negative exponents are
 * formatted using the localized minus sign, <em>not</em> the prefix and
 * suffix from the pattern. This allows patterns such as "0.###E0 m/s". To
 * prefix positive exponents with a localized plus sign, specify '+' between the
 * exponent and the digits: "0.###E+0" will produce formats "1E+1", "1E+0",
 * "1E-1", etc. (In localized patterns, use the localized plus sign rather than
 * '+'.)
 * <li>The minimum number of integer digits is achieved by adjusting the
 * exponent. Example: 0.00123 formatted with "00.###E0" yields "12.3E-4". This
 * only happens if there is no maximum number of integer digits. If there is a
 * maximum, then the minimum number of integer digits is fixed at one.
 * <li>The maximum number of integer digits, if present, specifies the exponent
 * grouping. The most common use of this is to generate <em>engineering
 * notation</em>,
 * in which the exponent is a multiple of three, e.g., "##0.###E0". The number
 * 12345 is formatted using "##0.###E0" as "12.345E3".
 * <li>When using scientific notation, the formatter controls the digit counts
 * using significant digits logic. The maximum number of significant digits
 * limits the total number of integer and fraction digits that will be shown in
 * the mantissa; it does not affect parsing. For example, 12345 formatted with
 * "##0.##E0" is "12.3E3". See the section on significant digits for more
 * details.
 * <li>The number of significant digits shown is determined as follows: If no
 * significant digits are used in the pattern then the minimum number of
 * significant digits shown is one, the maximum number of significant digits
 * shown is the sum of the <em>minimum integer</em> and
 * <em>maximum fraction</em> digits, and it is unaffected by the maximum
 * integer digits. If this sum is zero, then all significant digits are shown.
 * If significant digits are used in the pattern then the number of integer
 * digits is fixed at one and there is no exponent grouping.
 * <li>Exponential patterns may not contain grouping separators.
 * </ul>
 * <a name="sigdig"></a>
 * <h4> <strong><font color="red">NEW</font>&nbsp;</strong> Significant
 * Digits</h4>
 * <p>
 * {@code DecimalFormat} has two ways of controlling how many digits are
 * shown: (a) significant digit counts or (b) integer and fraction digit counts.
 * Integer and fraction digit counts are described above. When a formatter uses
 * significant digits counts, the number of integer and fraction digits is not
 * specified directly, and the formatter settings for these counts are ignored.
 * Instead, the formatter uses as many integer and fraction digits as required
 * to display the specified number of significant digits.
 * <h5>Examples:</h5>
 * <blockquote> <table border=0 cellspacing=3 cellpadding=0>
 * <tr bgcolor="#ccccff">
 * <th align="left">Pattern</th>
 * <th align="left">Minimum significant digits</th>
 * <th align="left">Maximum significant digits</th>
 * <th align="left">Number</th>
 * <th align="left">Output of format()</th>
 * </tr>
 * <tr valign="top">
 * <td>{@code @@@}
 * <td>3</td>
 * <td>3</td>
 * <td>12345</td>
 * <td>{@code 12300}</td>
 * </tr>
 * <tr valign="top" bgcolor="#eeeeff">
 * <td>{@code @@@}</td>
 * <td>3</td>
 * <td>3</td>
 * <td>0.12345</td>
 * <td>{@code 0.123}</td>
 * </tr>
 * <tr valign="top">
 * <td>{@code @@##}</td>
 * <td>2</td>
 * <td>4</td>
 * <td>3.14159</td>
 * <td>{@code 3.142}</td>
 * </tr>
 * <tr valign="top" bgcolor="#eeeeff">
 * <td>{@code @@##}</td>
 * <td>2</td>
 * <td>4</td>
 * <td>1.23004</td>
 * <td>{@code 1.23}</td>
 * </tr>
 * </table> </blockquote>
 * <ul>
 * <li>Significant digit counts may be expressed using patterns that specify a
 * minimum and maximum number of significant digits. These are indicated by the
 * {@code '@'} and {@code '#'} characters. The minimum number of significant
 * digits is the number of {@code '@'} characters. The maximum number of
 * significant digits is the number of {@code '@'} characters plus the number of
 * {@code '#'} characters following on the right. For example, the pattern
 * {@code "@@@"} indicates exactly 3 significant digits. The pattern
 * {@code "@##"} indicates from 1 to 3 significant digits. Trailing zero digits
 * to the right of the decimal separator are suppressed after the minimum number
 * of significant digits have been shown. For example, the pattern {@code "@##"}
 * formats the number 0.1203 as {@code "0.12"}.
 * <li>If a pattern uses significant digits, it may not contain a decimal
 * separator, nor the {@code '0'} pattern character. Patterns such as
 * {@code "@00"} or {@code "@.###"} are disallowed.
 * <li>Any number of {@code '#'} characters may be prepended to the left of the
 * leftmost {@code '@'} character. These have no effect on the minimum and
 * maximum significant digit counts, but may be used to position grouping
 * separators. For example, {@code "#,#@#"} indicates a minimum of one
 * significant digit, a maximum of two significant digits, and a grouping size
 * of three.
 * <li>In order to enable significant digits formatting, use a pattern
 * containing the {@code '@'} pattern character.
 * <li>In order to disable significant digits formatting, use a pattern that
 * does not contain the {@code '@'} pattern character.
 * <li>The number of significant digits has no effect on parsing.
 * <li>Significant digits may be used together with exponential notation. Such
 * patterns are equivalent to a normal exponential pattern with a minimum and
 * maximum integer digit count of one, a minimum fraction digit count of the
 * number of '@' characters in the pattern - 1, and a maximum fraction digit
 * count of the number of '@' and '#' characters in the pattern - 1. For
 * example, the pattern {@code "@@###E0"} is equivalent to {@code "0.0###E0"}.
 * <li>If significant digits are in use then the integer and fraction digit
 * counts, as set via the API, are ignored.
 * </ul>
 * <h4> <strong><font color="red">NEW</font>&nbsp;</strong> Padding</h4>
 * <p>
 * {@code DecimalFormat} supports padding the result of {@code format} to a
 * specific width. Padding may be specified either through the API or through
 * the pattern syntax. In a pattern, the pad escape character followed by a
 * single pad character causes padding to be parsed and formatted. The pad
 * escape character is '*' in unlocalized patterns. For example,
 * {@code "$*x#,##0.00"} formats 123 to {@code "$xx123.00"}, and 1234 to
 * {@code "$1,234.00"}.
 * <ul>
 * <li>When padding is in effect, the width of the positive subpattern,
 * including prefix and suffix, determines the format width. For example, in the
 * pattern {@code "* #0 o''clock"}, the format width is 10.</li>
 * <li>The width is counted in 16-bit code units (Java {@code char}s).</li>
 * <li>Some parameters which usually do not matter have meaning when padding is
 * used, because the pattern width is significant with padding. In the pattern "*
 * ##,##,#,##0.##", the format width is 14. The initial characters "##,##," do
 * not affect the grouping size or maximum integer digits, but they do affect
 * the format width.</li>
 * <li>Padding may be inserted at one of four locations: before the prefix,
 * after the prefix, before the suffix or after the suffix. If padding is
 * specified in any other location, {@link #applyPattern} throws an {@link
 * IllegalArgumentException}. If there is no prefix, before the prefix and after
 * the prefix are equivalent, likewise for the suffix.</li>
 * <li>When specified in a pattern, the 16-bit {@code char} immediately
 * following the pad escape is the pad character. This may be any character,
 * including a special pattern character. That is, the pad escape
 * <em>escapes</em> the following character. If there is no character after
 * the pad escape, then the pattern is illegal.</li>
 * </ul>
 *
 * <h4>Serialization</h4>
 * <p>
 * Features marked as <strong><font color="red">NEW</font></strong> and patterns that use
 * characters not documented above are unlikely to serialize/deserialize correctly.
 *
 * <h4>Synchronization</h4>
 * <p>
 * {@code DecimalFormat} objects are not synchronized. Multiple threads should
 * not access one formatter concurrently.
 *
 * @see Format
 * @see NumberFormat
 */
public class DecimalFormat extends NumberFormat {

    private static final long serialVersionUID = 864413376551465018L;

    private transient DecimalFormatSymbols symbols;

    private transient RoundingMode roundingMode = RoundingMode.HALF_EVEN;

    private transient com.ibm.icu.text.DecimalFormat icuDecimalFormat;

    /**
     * Constructs a new {@code DecimalFormat} for formatting and parsing numbers
     * for the user's default locale.
     * See "<a href="../util/Locale.html#default_locale">Be wary of the default locale</a>".
     */
    public DecimalFormat() {
        Locale locale = Locale.getDefault();
        this.symbols = new DecimalFormatSymbols(locale);
        initIcu(LocaleData.get(locale).numberPattern);
    }

    /**
     * Constructs a new {@code DecimalFormat} using the specified non-localized
     * pattern and the {@code DecimalFormatSymbols} for the user's default Locale.
     * See "<a href="../util/Locale.html#default_locale">Be wary of the default locale</a>".
     * @param pattern
     *            the non-localized pattern.
     * @throws IllegalArgumentException
     *            if the pattern cannot be parsed.
     */
    public DecimalFormat(String pattern) {
        this(pattern, Locale.getDefault());
    }

    /**
     * Constructs a new {@code DecimalFormat} using the specified non-localized
     * pattern and {@code DecimalFormatSymbols}.
     *
     * @param pattern
     *            the non-localized pattern.
     * @param value
     *            the DecimalFormatSymbols.
     * @throws IllegalArgumentException
     *            if the pattern cannot be parsed.
     */
    public DecimalFormat(String pattern, DecimalFormatSymbols value) {
        this.symbols = (DecimalFormatSymbols) value.clone();
        initIcu(pattern);
    }

    // Used by NumberFormat.getInstance because cloning DecimalFormatSymbols is slow.
    DecimalFormat(String pattern, Locale locale) {
        this.symbols = new DecimalFormatSymbols(locale);
        initIcu(pattern);
    }

    // Path for instances created with a locale and no pattern. This ensures that in locales with
    // specific number formatting needs such as the case with ar_AE which has a minus sign of more
    // than one char, we get the correct pattern which contains the left to right symbol and the
    // minus sign.
    DecimalFormat(com.ibm.icu.text.DecimalFormat icuDF) {
        this.icuDecimalFormat = icuDF;

        this.symbols = DecimalFormatSymbols.fromIcuInstance(icuDF.getDecimalFormatSymbols());
        super.setMaximumFractionDigits(icuDecimalFormat.getMaximumFractionDigits());
        super.setMaximumIntegerDigits(icuDecimalFormat.getMaximumIntegerDigits());
        super.setMinimumFractionDigits(icuDecimalFormat.getMinimumFractionDigits());
        super.setMinimumIntegerDigits(icuDecimalFormat.getMinimumIntegerDigits());
    }

    private void initIcu(String pattern) {
        this.icuDecimalFormat = new com.ibm.icu.text.DecimalFormat(
                pattern, symbols.getIcuDecimalFormatSymbols());
        super.setMaximumFractionDigits(icuDecimalFormat.getMaximumFractionDigits());
        super.setMaximumIntegerDigits(icuDecimalFormat.getMaximumIntegerDigits());
        super.setMinimumFractionDigits(icuDecimalFormat.getMinimumFractionDigits());
        super.setMinimumIntegerDigits(icuDecimalFormat.getMinimumIntegerDigits());
    }

    /**
     * Changes the pattern of this decimal format to the specified pattern which
     * uses localized pattern characters.
     *
     * @param pattern
     *            the localized pattern.
     * @throws IllegalArgumentException
     *            if the pattern cannot be parsed.
     */
    public void applyLocalizedPattern(String pattern) {
        icuDecimalFormat.applyLocalizedPattern(pattern);
        updateFieldsFromIcu();
    }

    /**
     * Changes the pattern of this decimal format to the specified pattern which
     * uses non-localized pattern characters.
     *
     * @param pattern
     *            the non-localized pattern.
     * @throws IllegalArgumentException
     *            if the pattern cannot be parsed.
     */
    public void applyPattern(String pattern) {
        // The underlying ICU4J accepts a super-set of the pattern spec documented by the Android
        // APIs. For example, rounding increments (pattern characters '1'-'9'). They will work but
        // see class doc for issues with serialization/deserialization they may cause.
        icuDecimalFormat.applyPattern(pattern);
        updateFieldsFromIcu();
    }

    private void updateFieldsFromIcu() {
        maximumIntegerDigits = icuDecimalFormat.getMaximumIntegerDigits();
        minimumIntegerDigits = icuDecimalFormat.getMinimumIntegerDigits();
        maximumFractionDigits = icuDecimalFormat.getMaximumFractionDigits();
        minimumFractionDigits = icuDecimalFormat.getMinimumFractionDigits();
    }

    /**
     * Returns a new instance of {@code DecimalFormat} with the same pattern and
     * properties.
     */
    @Override
    public Object clone() {
        DecimalFormat clone = (DecimalFormat) super.clone();
        clone.icuDecimalFormat = (com.ibm.icu.text.DecimalFormat) icuDecimalFormat.clone();
        clone.symbols = (DecimalFormatSymbols) symbols.clone();
        return clone;
    }

    /**
     * Compares the specified object to this decimal format and indicates if
     * they are equal. In order to be equal, {@code object} must be an instance
     * of {@code DecimalFormat} with the same pattern and properties.
     *
     * @param object
     *            the object to compare with this object.
     * @return {@code true} if the specified object is equal to this decimal
     *         format; {@code false} otherwise.
     * @see #hashCode
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DecimalFormat)) {
            return false;
        }
        DecimalFormat other = (DecimalFormat) object;

        if (this.icuDecimalFormat == null) {
            return other.icuDecimalFormat == null;
        }

        // The following is sufficient as ICU checks the sub-patterns which get data from the
        // parsed pattern, as well as the internal symbols set on the DecimalFormat object.
        // ICU does not, however check the rounding increment in its .equals(). Thus we check that
        // after ICU's equals has returned true.
        return this.icuDecimalFormat.equals(other.icuDecimalFormat)
                && compareIcuRoundingIncrement(other.icuDecimalFormat);
    }

    private boolean compareIcuRoundingIncrement(com.ibm.icu.text.DecimalFormat other) {
        BigDecimal increment = this.icuDecimalFormat.getRoundingIncrement();
        if (increment != null) {
            return (other.getRoundingIncrement() != null)
                    && increment.equals(other.getRoundingIncrement());
        }
        return other.getRoundingIncrement() == null;
    }

    /**
     * Formats the specified object using the rules of this decimal format and
     * returns an {@code AttributedCharacterIterator} with the formatted number
     * and attributes.
     *
     * @param object
     *            the object to format.
     * @return an AttributedCharacterIterator with the formatted number and
     *         attributes.
     * @throws IllegalArgumentException
     *             if {@code object} cannot be formatted by this format.
     * @throws NullPointerException
     *             if {@code object} is {@code null}.
     */
    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object object) {
        if (object == null) {
            throw new NullPointerException("object == null");
        }
        // Note: formatToCharacterIterator cannot be used directly because it returns attributes
        // in terms of its own class: icu.text.NumberFormat instead of java.text.NumberFormat.
        // http://bugs.icu-project.org/trac/ticket/11931 Proposes to use the NumberFormat constants.

        AttributedCharacterIterator original = icuDecimalFormat.formatToCharacterIterator(object);

        // Extract the text out of the ICU iterator.
        StringBuilder textBuilder = new StringBuilder(
                original.getEndIndex() - original.getBeginIndex());

        for (int i = original.getBeginIndex(); i < original.getEndIndex(); i++) {
            textBuilder.append(original.current());
            original.next();
        }

        AttributedString result = new AttributedString(textBuilder.toString());

        for (int i = original.getBeginIndex(); i < original.getEndIndex(); i++) {
            original.setIndex(i);

            for (AttributedCharacterIterator.Attribute attribute
                    : original.getAttributes().keySet()) {
                    int start = original.getRunStart();
                    int end = original.getRunLimit();
                    Field javaAttr = toJavaFieldAttribute(attribute);
                    result.addAttribute(javaAttr, javaAttr, start, end);
            }
        }

        return result.getIterator();
    }

    private void checkBufferAndFieldPosition(StringBuffer buffer, FieldPosition position) {
        if (buffer == null) {
            throw new NullPointerException("buffer == null");
        }
        if (position == null) {
            throw new NullPointerException("position == null");
        }
    }

    @Override
    public StringBuffer format(double value, StringBuffer buffer, FieldPosition position) {
        FieldPosition icuFieldPosition = getIcuFieldPosition(position);
        checkBufferAndFieldPosition(buffer, position);
        icuDecimalFormat.format(value, buffer, icuFieldPosition);
        position.setBeginIndex(icuFieldPosition.getBeginIndex());
        position.setEndIndex(icuFieldPosition.getEndIndex());
        return buffer;
    }

    @Override
    public StringBuffer format(long value, StringBuffer buffer, FieldPosition position) {
        FieldPosition icuFieldPosition = getIcuFieldPosition(position);
        checkBufferAndFieldPosition(buffer, position);
        icuDecimalFormat.format(value, buffer, icuFieldPosition);
        position.setBeginIndex(icuFieldPosition.getBeginIndex());
        position.setEndIndex(icuFieldPosition.getEndIndex());
        return buffer;
    }

    @Override
    public final StringBuffer format(Object number, StringBuffer buffer, FieldPosition position) {
        checkBufferAndFieldPosition(buffer, position);
        FieldPosition icuFieldPosition = getIcuFieldPosition(position);
        if (number instanceof BigInteger) {
            BigInteger bigInteger = (BigInteger) number;
            icuDecimalFormat.format(
                    (bigInteger.bitLength() < 64) ? bigInteger.longValue() : bigInteger,
                    buffer, icuFieldPosition);
            position.setBeginIndex(icuFieldPosition.getBeginIndex());
            position.setEndIndex(icuFieldPosition.getEndIndex());
           return buffer;
        } else if (number instanceof BigDecimal) {
            icuDecimalFormat.format((BigDecimal) number, buffer, icuFieldPosition);
            position.setBeginIndex(icuFieldPosition.getBeginIndex());
            position.setEndIndex(icuFieldPosition.getEndIndex());
            return buffer;
        }
        return super.format(number, buffer, position);
    }


    /**
     * Converts between field positions used by Java/ICU.
     * @param fp The java.text.NumberFormat.Field field position
     * @return The com.ibm.icu.text.NumberFormat.Field field position
     */
    private static FieldPosition getIcuFieldPosition(FieldPosition fp) {
        if (fp.getFieldAttribute() == null) return fp;

        com.ibm.icu.text.NumberFormat.Field attribute;
        if (fp.getFieldAttribute() == Field.INTEGER) {
            attribute = com.ibm.icu.text.NumberFormat.Field.INTEGER;
        } else if (fp.getFieldAttribute() == Field.FRACTION) {
            attribute = com.ibm.icu.text.NumberFormat.Field.FRACTION;
        } else if (fp.getFieldAttribute() == Field.DECIMAL_SEPARATOR) {
            attribute = com.ibm.icu.text.NumberFormat.Field.DECIMAL_SEPARATOR;
        } else if (fp.getFieldAttribute() == Field.EXPONENT_SYMBOL) {
            attribute = com.ibm.icu.text.NumberFormat.Field.EXPONENT_SYMBOL;
        } else if (fp.getFieldAttribute() == Field.EXPONENT_SIGN) {
            attribute = com.ibm.icu.text.NumberFormat.Field.EXPONENT_SIGN;
        } else if (fp.getFieldAttribute() == Field.EXPONENT) {
            attribute = com.ibm.icu.text.NumberFormat.Field.EXPONENT;
        } else if (fp.getFieldAttribute() == Field.GROUPING_SEPARATOR) {
            attribute = com.ibm.icu.text.NumberFormat.Field.GROUPING_SEPARATOR;
        } else if (fp.getFieldAttribute() == Field.CURRENCY) {
            attribute = com.ibm.icu.text.NumberFormat.Field.CURRENCY;
        } else if (fp.getFieldAttribute() == Field.PERCENT) {
            attribute = com.ibm.icu.text.NumberFormat.Field.PERCENT;
        } else if (fp.getFieldAttribute() == Field.PERMILLE) {
            attribute = com.ibm.icu.text.NumberFormat.Field.PERMILLE;
        } else if (fp.getFieldAttribute() == Field.SIGN) {
            attribute = com.ibm.icu.text.NumberFormat.Field.SIGN;
        } else {
            throw new IllegalArgumentException("Unexpected field position attribute type.");
        }

        FieldPosition icuFieldPosition = new FieldPosition(attribute);
        icuFieldPosition.setBeginIndex(fp.getBeginIndex());
        icuFieldPosition.setEndIndex(fp.getEndIndex());
        return icuFieldPosition;
    }

    /**
     * Converts the Attribute that ICU returns in its AttributedCharacterIterator
     * responses to the type that java uses.
     * @param icuAttribute The com.ibm.icu.text.NumberFormat.Field field.
     * @return Field converted to a java.text.NumberFormat.Field field.
     */
    private static Field toJavaFieldAttribute(AttributedCharacterIterator.Attribute icuAttribute) {
        if (icuAttribute.getName().equals(Field.INTEGER.getName())) {
            return Field.INTEGER;
        }
        if (icuAttribute.getName().equals(Field.CURRENCY.getName())) {
            return Field.CURRENCY;
        }
        if (icuAttribute.getName().equals(Field.DECIMAL_SEPARATOR.getName())) {
            return Field.DECIMAL_SEPARATOR;
        }
        if (icuAttribute.getName().equals(Field.EXPONENT.getName())) {
            return Field.EXPONENT;
        }
        if (icuAttribute.getName().equals(Field.EXPONENT_SIGN.getName())) {
            return Field.EXPONENT_SIGN;
        }
        if (icuAttribute.getName().equals(Field.EXPONENT_SYMBOL.getName())) {
            return Field.EXPONENT_SYMBOL;
        }
        if (icuAttribute.getName().equals(Field.FRACTION.getName())) {
            return Field.FRACTION;
        }
        if (icuAttribute.getName().equals(Field.GROUPING_SEPARATOR.getName())) {
            return Field.GROUPING_SEPARATOR;
        }
        if (icuAttribute.getName().equals(Field.SIGN.getName())) {
            return Field.SIGN;
        }
        if (icuAttribute.getName().equals(Field.PERCENT.getName())) {
            return Field.PERCENT;
        }
        if (icuAttribute.getName().equals(Field.PERMILLE.getName())) {
            return Field.PERMILLE;
        }
        throw new IllegalArgumentException("Unrecognized attribute: " + icuAttribute.getName());
   }


    /**
     * Returns the {@code DecimalFormatSymbols} used by this decimal format.
     *
     * @return a copy of the {@code DecimalFormatSymbols} used by this decimal
     *         format.
     */
    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return DecimalFormatSymbols.fromIcuInstance(icuDecimalFormat.getDecimalFormatSymbols());
    }

    /**
     * Returns the currency used by this decimal format.
     *
     * @return the currency used by this decimal format.
     * @see DecimalFormatSymbols#getCurrency()
     */
    @Override
    public Currency getCurrency() {
        return symbols.getCurrency();
    }

    /**
     * Returns the number of digits grouped together by the grouping separator.
     * This only allows to get the primary grouping size. There is no API to get
     * the secondary grouping size.
     *
     * @return the number of digits grouped together.
     */
    public int getGroupingSize() {
        return icuDecimalFormat.getGroupingSize();
    }

    /**
     * Returns the prefix which is formatted or parsed before a negative number.
     *
     * @return the negative prefix.
     */
    public String getNegativePrefix() {
        return icuDecimalFormat.getNegativePrefix();
    }

    /**
     * Returns the suffix which is formatted or parsed after a negative number.
     *
     * @return the negative suffix.
     */
    public String getNegativeSuffix() {
        return icuDecimalFormat.getNegativeSuffix();
    }

    /**
     * Returns the prefix which is formatted or parsed before a positive number.
     *
     * @return the positive prefix.
     */
    public String getPositivePrefix() {
        return icuDecimalFormat.getPositivePrefix();
    }

    /**
     * Returns the suffix which is formatted or parsed after a positive number.
     *
     * @return the positive suffix.
     */
    public String getPositiveSuffix() {
        return icuDecimalFormat.getPositiveSuffix();
    }

    @Override
    public int hashCode() {
        return getPositivePrefix().hashCode();
    }

    /**
     * Indicates whether the decimal separator is shown when there are no
     * fractional digits.
     *
     * @return {@code true} if the decimal separator should always be formatted;
     *         {@code false} otherwise.
     */
    public boolean isDecimalSeparatorAlwaysShown() {
        return icuDecimalFormat.isDecimalSeparatorAlwaysShown();
    }

    /**
     * This value indicates whether the return object of the parse operation is
     * of type {@code BigDecimal}. This value defaults to {@code false}.
     *
     * @return {@code true} if parse always returns {@code BigDecimals},
     *         {@code false} if the type of the result is {@code Long} or
     *         {@code Double}.
     */
    public boolean isParseBigDecimal() {
        return icuDecimalFormat.isParseBigDecimal();
    }

    /**
     * Sets the flag that indicates whether numbers will be parsed as integers.
     * When this decimal format is used for parsing and this value is set to
     * {@code true}, then the resulting numbers will be of type
     * {@code java.lang.Integer}. Special cases are NaN, positive and negative
     * infinity, which are still returned as {@code java.lang.Double}.
     *
     *
     * @param value
     *            {@code true} that the resulting numbers of parse operations
     *            will be of type {@code java.lang.Integer} except for the
     *            special cases described above.
     */
    @Override
    public void setParseIntegerOnly(boolean value) {
        // In this implementation, NativeDecimalFormat is wrapped to
        // fulfill most of the format and parse feature. And this method is
        // delegated to the wrapped instance of NativeDecimalFormat.
        super.setParseIntegerOnly(value);
        icuDecimalFormat.setParseIntegerOnly(value);
    }

    @Override
    public boolean isParseIntegerOnly() {
        return icuDecimalFormat.isParseIntegerOnly();
    }

    private static final Double NEGATIVE_ZERO_DOUBLE = new Double(-0.0);

    /**
     * Parses a {@code Long} or {@code Double} from the specified string
     * starting at the index specified by {@code position}. If the string is
     * successfully parsed then the index of the {@code ParsePosition} is
     * updated to the index following the parsed text. On error, the index is
     * unchanged and the error index of {@code ParsePosition} is set to the
     * index where the error occurred.
     *
     * @param string
     *            the string to parse.
     * @param position
     *            input/output parameter, specifies the start index in
     *            {@code string} from where to start parsing. If parsing is
     *            successful, it is updated with the index following the parsed
     *            text; on error, the index is unchanged and the error index is
     *            set to the index where the error occurred.
     * @return a {@code Long} or {@code Double} resulting from the parse or
     *         {@code null} if there is an error. The result will be a
     *         {@code Long} if the parsed number is an integer in the range of a
     *         long, otherwise the result is a {@code Double}. If
     *         {@code isParseBigDecimal} is {@code true} then it returns the
     *         result as a {@code BigDecimal}.
     */
    @Override
    public Number parse(String string, ParsePosition position) {
        // This might return com.ibm.icu.math.BigDecimal, java.math.BigInteger or a primitive type.
        Number number = icuDecimalFormat.parse(string, position);
        if (number == null) {
            return null;
        }
        if (this.isParseBigDecimal()) {
            if (number instanceof Long) {
                return new BigDecimal(number.longValue());
            }
            if ((number instanceof Double) && !((Double) number).isInfinite()
                    && !((Double) number).isNaN()) {

                return new BigDecimal(number.toString());
            }
            if ((number instanceof Double) &&
                    (((Double) number).isNaN() || ((Double) number).isInfinite())) {
                return number;
            }
            if (number instanceof com.ibm.icu.math.BigDecimal) {
                return ((com.ibm.icu.math.BigDecimal) number).toBigDecimal();
            }
        }
        if ((number instanceof com.ibm.icu.math.BigDecimal) || (number instanceof BigInteger)) {
            return number.doubleValue();
        }
        if (this.isParseIntegerOnly() && number.equals(NEGATIVE_ZERO_DOUBLE)) {
            return 0L;
        }
        return number;
    }

    /**
     * Sets the {@code DecimalFormatSymbols} used by this decimal format.
     */
    public void setDecimalFormatSymbols(DecimalFormatSymbols value) {
        if (value != null) {
            this.symbols = (DecimalFormatSymbols) value.clone();
            icuDecimalFormat.setDecimalFormatSymbols(symbols.getIcuDecimalFormatSymbols());
        }
    }

    /**
     * Sets the currency used by this decimal format. The min and max fraction
     * digits remain the same.
     */
    @Override
    public void setCurrency(Currency currency) {
        // Set the international currency symbol, and currency symbol on the DecimalFormatSymbols
        // object and tell ICU to use that. Trying to set the currency on icuDecimalFormat will
        // cause the fractional digits to be updated.
        Currency instance = Currency.getInstance(currency.getCurrencyCode());
        if (instance != null) {
            symbols.setCurrency(instance);
        }

        icuDecimalFormat.setDecimalFormatSymbols(symbols.getIcuDecimalFormatSymbols());
    }

    /**
     * Sets whether the decimal separator is shown even when there are no fractional
     * digits.
     */
    public void setDecimalSeparatorAlwaysShown(boolean value) {
        icuDecimalFormat.setDecimalSeparatorAlwaysShown(value);
    }

    /**
     * Sets the number of digits grouped together by the grouping separator.
     * This only allows to set the primary grouping size; the secondary grouping
     * size can only be set with a pattern.
     */
    public void setGroupingSize(int value) {
        icuDecimalFormat.setGroupingSize(value);
    }

    /**
     * Sets whether or not digit grouping will be used in this format. Grouping
     * affects both formatting and parsing.
     */
    @Override
    public void setGroupingUsed(boolean value) {
        icuDecimalFormat.setGroupingUsed(value);
    }

    /**
     * Returns true if digit grouping is used in this format. Grouping affects both
     * formatting and parsing.
     */
    @Override
    public boolean isGroupingUsed() {
        return icuDecimalFormat.isGroupingUsed();
    }

    /**
     * Sets the maximum number of digits after the decimal point.
     * If the value passed is negative then it is replaced by 0.
     * Regardless of this setting, no more than 340 digits will be used.
     */
    @Override
    public void setMaximumFractionDigits(int value) {
        super.setMaximumFractionDigits(value);
        icuDecimalFormat.setMaximumFractionDigits(getMaximumFractionDigits());
        // Changing the maximum fraction digits needs to update ICU4J's rounding configuration.
        setRoundingMode(roundingMode);
    }

    /**
     * Sets the maximum number of digits before the decimal point.
     * If the value passed is negative then it is replaced by 0.
     * Regardless of this setting, no more than 309 digits will be used.
     */
    @Override
    public void setMaximumIntegerDigits(int value) {
        super.setMaximumIntegerDigits(value);
        icuDecimalFormat.setMaximumIntegerDigits(getMaximumIntegerDigits());
    }

    /**
     * Sets the minimum number of digits after the decimal point.
     * If the value passed is negative then it is replaced by 0.
     * Regardless of this setting, no more than 340 digits will be used.
     */
    @Override
    public void setMinimumFractionDigits(int value) {
        super.setMinimumFractionDigits(value);
        icuDecimalFormat.setMinimumFractionDigits(getMinimumFractionDigits());
    }

    /**
     * Sets the minimum number of digits before the decimal point.
     * If the value passed is negative then it is replaced by 0.
     * Regardless of this setting, no more than 309 digits will be used.
     */
    @Override
    public void setMinimumIntegerDigits(int value) {
        super.setMinimumIntegerDigits(value);
        icuDecimalFormat.setMinimumIntegerDigits(getMinimumIntegerDigits());
    }

    /**
     * Returns the multiplier which is applied to the number before formatting
     * or after parsing. The multiplier is meant for tasks like parsing percentages.
     * For example, given a multiplier of 100, 1.23 would be formatted as "123" and
     * "123" would be parsed as 1.23.
     */
    public int getMultiplier() {
        return icuDecimalFormat.getMultiplier();
    }

    /**
     * Sets the multiplier which is applied to the number before formatting or
     * after parsing. The multiplier meant for tasks like parsing percentages.
     * For example, given a multiplier of 100, 1.23 would be formatted as "123" and
     * "123" would be parsed as 1.23.
     */
    public void setMultiplier(int value) {
        icuDecimalFormat.setMultiplier(value);
    }

    /**
     * Sets the prefix which is formatted or parsed before a negative number.
     */
    public void setNegativePrefix(String value) {
        icuDecimalFormat.setNegativePrefix(value);
    }

    /**
     * Sets the suffix which is formatted or parsed after a negative number.
     */
    public void setNegativeSuffix(String value) {
        icuDecimalFormat.setNegativeSuffix(value);
    }

    /**
     * Sets the prefix which is formatted or parsed before a positive number.
     */
    public void setPositivePrefix(String value) {
        icuDecimalFormat.setPositivePrefix(value);
    }

    /**
     * Sets the suffix which is formatted or parsed after a positive number.
     */
    public void setPositiveSuffix(String value) {
        icuDecimalFormat.setPositiveSuffix(value);
    }

    /**
     * Sets the behavior of the parse method. If set to {@code true} then all
     * the returned objects will be of type {@code BigDecimal}.
     *
     * @param newValue
     *            {@code true} if all the returned objects should be of type
     *            {@code BigDecimal}; {@code false} otherwise.
     */
    public void setParseBigDecimal(boolean newValue) {
        icuDecimalFormat.setParseBigDecimal(newValue);
    }

    /**
     * Returns the pattern of this decimal format using localized pattern
     * characters.
     *
     * @return the localized pattern.
     */
    public String toLocalizedPattern() {
        return icuDecimalFormat.toLocalizedPattern();
    }

    /**
     * Returns the pattern of this decimal format using non-localized pattern
     * characters.
     *
     * @return the non-localized pattern.
     */
    public String toPattern() {
        return icuDecimalFormat.toPattern();
    }

    // the fields list to be serialized
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("positivePrefix", String.class),
        new ObjectStreamField("positiveSuffix", String.class),
        new ObjectStreamField("negativePrefix", String.class),
        new ObjectStreamField("negativeSuffix", String.class),
        new ObjectStreamField("posPrefixPattern", String.class),
        new ObjectStreamField("posSuffixPattern", String.class),
        new ObjectStreamField("negPrefixPattern", String.class),
        new ObjectStreamField("negSuffixPattern", String.class),
        new ObjectStreamField("multiplier", int.class),
        new ObjectStreamField("groupingSize", byte.class),
        new ObjectStreamField("groupingUsed", boolean.class),
        new ObjectStreamField("decimalSeparatorAlwaysShown", boolean.class),
        new ObjectStreamField("parseBigDecimal", boolean.class),
        new ObjectStreamField("roundingMode", RoundingMode.class),
        new ObjectStreamField("symbols", DecimalFormatSymbols.class),
        new ObjectStreamField("useExponentialNotation", boolean.class),
        new ObjectStreamField("minExponentDigits", byte.class),
        new ObjectStreamField("maximumIntegerDigits", int.class),
        new ObjectStreamField("minimumIntegerDigits", int.class),
        new ObjectStreamField("maximumFractionDigits", int.class),
        new ObjectStreamField("minimumFractionDigits", int.class),
        new ObjectStreamField("serialVersionOnStream", int.class),
    };

    /**
     * Writes serialized fields following serialized forms specified by Java
     * specification.
     *
     * @param stream
     *            the output stream to write serialized bytes
     * @throws IOException
     *             if some I/O error occurs
     * @throws ClassNotFoundException
     */
    private void writeObject(ObjectOutputStream stream) throws IOException, ClassNotFoundException {
        ObjectOutputStream.PutField fields = stream.putFields();
        fields.put("positivePrefix", icuDecimalFormat.getPositivePrefix());
        fields.put("positiveSuffix", icuDecimalFormat.getPositiveSuffix());
        fields.put("negativePrefix", icuDecimalFormat.getNegativePrefix());
        fields.put("negativeSuffix", icuDecimalFormat.getNegativeSuffix());
        fields.put("posPrefixPattern", null);
        fields.put("posSuffixPattern", null);
        fields.put("negPrefixPattern", null);
        fields.put("negSuffixPattern", null);
        fields.put("multiplier", icuDecimalFormat.getMultiplier());
        fields.put("groupingSize", (byte) icuDecimalFormat.getGroupingSize());
        fields.put("groupingUsed", icuDecimalFormat.isGroupingUsed());
        fields.put("decimalSeparatorAlwaysShown", icuDecimalFormat.isDecimalSeparatorAlwaysShown());
        fields.put("parseBigDecimal", icuDecimalFormat.isParseBigDecimal());
        fields.put("roundingMode", roundingMode);
        fields.put("symbols", symbols);
        fields.put("useExponentialNotation", false);
        fields.put("minExponentDigits", (byte) 0);
        fields.put("maximumIntegerDigits", icuDecimalFormat.getMaximumIntegerDigits());
        fields.put("minimumIntegerDigits", icuDecimalFormat.getMinimumIntegerDigits());
        fields.put("maximumFractionDigits", icuDecimalFormat.getMaximumFractionDigits());
        fields.put("minimumFractionDigits", icuDecimalFormat.getMinimumFractionDigits());
        fields.put("serialVersionOnStream", 4);
        stream.writeFields();
    }

    /**
     * Reads serialized fields following serialized forms specified by Java
     * specification.
     *
     * @param stream
     *            the input stream to read serialized bytes
     * @throws IOException
     *             if some I/O error occurs
     * @throws ClassNotFoundException
     *             if some class of serialized objects or fields cannot be found
     */
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = stream.readFields();
        this.symbols = (DecimalFormatSymbols) fields.get("symbols", null);

        initIcu("");

        // Calling setPositive<Prefix|Suffix> will cause quotes to be inserted at the beginning
        // or end of the pattern, so only call it if there is actually something to put there.
        String positivePrefix = (String) fields.get("positivePrefix", "");
        if (!positivePrefix.equals("")) {
            setPositivePrefix(positivePrefix);
        }

        String positiveSuffix = (String) fields.get("positiveSuffix", "");
        if (!positiveSuffix.equals("")) {
            setPositiveSuffix(positiveSuffix);
        }

        setNegativePrefix((String) fields.get("negativePrefix", "-"));
        setNegativeSuffix((String) fields.get("negativeSuffix", ""));

        setMultiplier(fields.get("multiplier", 1));
        setGroupingSize(fields.get("groupingSize", (byte) 3));
        setGroupingUsed(fields.get("groupingUsed", true));
        setDecimalSeparatorAlwaysShown(fields.get("decimalSeparatorAlwaysShown", false));

        setRoundingMode((RoundingMode) fields.get("roundingMode", RoundingMode.HALF_EVEN));

        final int maximumIntegerDigits = fields.get("maximumIntegerDigits", 309);
        final int minimumIntegerDigits = fields.get("minimumIntegerDigits", 309);
        final int maximumFractionDigits = fields.get("maximumFractionDigits", 340);
        final int minimumFractionDigits = fields.get("minimumFractionDigits", 340);
        // ICU has its own logic about what these values can be set to. We set the desired value
        // on icuDecimalFormat and then update NumberFormat's idea of the limits to what ICU has
        // allowed to be set. This isn't RI-compatible, but then very little of our
        // behavior in this area is, and it's not obvious how we can second-guess ICU (or tell
        // it to just do exactly what we ask).
        icuDecimalFormat.setMaximumIntegerDigits(maximumIntegerDigits);
        setMaximumIntegerDigits(icuDecimalFormat.getMaximumIntegerDigits());

        icuDecimalFormat.setMinimumIntegerDigits(minimumIntegerDigits);
        setMinimumIntegerDigits(icuDecimalFormat.getMinimumIntegerDigits());

        icuDecimalFormat.setMaximumFractionDigits(maximumFractionDigits);
        setMaximumFractionDigits(icuDecimalFormat.getMaximumFractionDigits());

        icuDecimalFormat.setMinimumFractionDigits(minimumFractionDigits);
        setMinimumFractionDigits(minimumFractionDigits);

        setParseBigDecimal(fields.get("parseBigDecimal", false));

        if (fields.get("serialVersionOnStream", 0) < 3) {
            setMaximumIntegerDigits(super.getMaximumIntegerDigits());
            setMinimumIntegerDigits(super.getMinimumIntegerDigits());
            setMaximumFractionDigits(super.getMaximumFractionDigits());
            setMinimumFractionDigits(super.getMinimumFractionDigits());
        }
    }

    /**
     * Returns the {@code RoundingMode} used by this {@code NumberFormat}.
     * @since 1.6
     */
    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    /**
     * Convert RoundingMode into an integer representation which ICU's DecimalFormat understands.
     * @param rm RoundingMode to use.
     * @return an integer representing a RoundingMode for ICU.
     */
    private static int convertRoundingMode(RoundingMode rm) {
        switch (rm) {
            case UP:
                return MathContext.ROUND_UP;
            case DOWN:
                return MathContext.ROUND_DOWN;
            case CEILING:
                return MathContext.ROUND_CEILING;
            case FLOOR:
                return MathContext.ROUND_FLOOR;
            case HALF_UP:
                return MathContext.ROUND_HALF_UP;
            case HALF_DOWN:
                return MathContext.ROUND_HALF_DOWN;
            case HALF_EVEN:
                return MathContext.ROUND_HALF_EVEN;
            case UNNECESSARY:
                return MathContext.ROUND_UNNECESSARY;
        }
        throw new IllegalArgumentException("Invalid rounding mode specified");
    }

    /**
     * Sets the {@code RoundingMode} used by this {@code NumberFormat}.
     * @since 1.6
     */
    public void setRoundingMode(RoundingMode roundingMode) {
        if (roundingMode == null) {
            throw new NullPointerException("roundingMode == null");
        }
        this.roundingMode = roundingMode;
        // DecimalFormat does not allow specification of a rounding increment.  If anything other
        // than 0.0 is used here the resulting DecimalFormat cannot be deserialized because the
        // serialization format does not include rounding increment information.
        icuDecimalFormat.setRoundingMode(convertRoundingMode(roundingMode));
    }

    public String toString() {
        return getClass().getName() + "[\"" + toPattern() + "\"" +
                ",isDecimalSeparatorAlwaysShown=" + isDecimalSeparatorAlwaysShown() +
                ",groupingSize=" + getGroupingSize() +
                ",multiplier=" + getMultiplier() +
                ",negativePrefix=" + getNegativePrefix() +
                ",negativeSuffix=" + getNegativeSuffix() +
                ",positivePrefix=" + getPositivePrefix() +
                ",positiveSuffix=" + getPositiveSuffix() +
                ",maxIntegerDigits=" + getMaximumIntegerDigits() +
                ",maxFractionDigits=" + getMaximumFractionDigits() +
                ",minIntegerDigits=" + getMinimumIntegerDigits() +
                ",minFractionDigits=" + getMinimumFractionDigits() +
                ",grouping=" + isGroupingUsed() +
                "]";
    }
}
