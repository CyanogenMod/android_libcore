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

package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import libcore.icu.ICU;

/**
 * {@code Locale} represents a language/country/variant combination. Locales are used to
 * alter the presentation of information such as numbers or dates to suit the conventions
 * in the region they describe.
 *
 * <p>The language codes are two-letter lowercase ISO language codes (such as "en") as defined by
 * <a href="http://en.wikipedia.org/wiki/ISO_639-1">ISO 639-1</a>.
 * The country codes are two-letter uppercase ISO country codes (such as "US") as defined by
 * <a href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-3">ISO 3166-1</a>.
 * The variant codes are unspecified.
 *
 * <p>Note that Java uses several deprecated two-letter codes. The Hebrew ("he") language
 * code is rewritten as "iw", Indonesian ("id") as "in", and Yiddish ("yi") as "ji". This
 * rewriting happens even if you construct your own {@code Locale} object, not just for
 * instances returned by the various lookup methods.
 *
 * <a name="available_locales"><h3>Available locales</h3></a>
 * <p>This class' constructors do no error checking. You can create a {@code Locale} for languages
 * and countries that don't exist, and you can create instances for combinations that don't
 * exist (such as "de_US" for "German as spoken in the US").
 *
 * <p>Note that locale data is not necessarily available for any of the locales pre-defined as
 * constants in this class except for en_US, which is the only locale Java guarantees is always
 * available.
 *
 * <p>It is also a mistake to assume that all devices have the same locales available.
 * A device sold in the US will almost certainly support en_US and es_US, but not necessarily
 * any locales with the same language but different countries (such as en_GB or es_ES),
 * nor any locales for other languages (such as de_DE). The opposite may well be true for a device
 * sold in Europe.
 *
 * <p>You can use {@link Locale#getDefault} to get an appropriate locale for the <i>user</i> of the
 * device you're running on, or {@link Locale#getAvailableLocales} to get a list of all the locales
 * available on the device you're running on.
 *
 * <a name="locale_data"><h3>Locale data</h3></a>
 * <p>Note that locale data comes solely from ICU. User-supplied locale service providers (using
 * the {@code java.text.spi} or {@code java.util.spi} mechanisms) are not supported.
 *
 * <p>Here are the versions of ICU (and the corresponding CLDR and Unicode versions) used in
 * various Android releases:
 * <table BORDER="1" WIDTH="100%" CELLPADDING="3" CELLSPACING="0" SUMMARY="">
 * <tr><td>Android 1.5 (Cupcake)/Android 1.6 (Donut)/Android 2.0 (Eclair)</td>
 *     <td>ICU 3.8</td>
 *     <td><a href="http://cldr.unicode.org/index/downloads/cldr-1-5">CLDR 1.5</a></td>
 *     <td><a href="http://www.unicode.org/versions/Unicode5.0.0/">Unicode 5.0</a></td></tr>
 * <tr><td>Android 2.2 (Froyo)</td>
 *     <td>ICU 4.2</td>
 *     <td><a href="http://cldr.unicode.org/index/downloads/cldr-1-7">CLDR 1.7</a></td>
 *     <td><a href="http://www.unicode.org/versions/Unicode5.1.0/">Unicode 5.1</a></td></tr>
 * <tr><td>Android 2.3 (Gingerbread)/Android 3.0 (Honeycomb)</td>
 *     <td>ICU 4.4</td>
 *     <td><a href="http://cldr.unicode.org/index/downloads/cldr-1-8">CLDR 1.8</a></td>
 *     <td><a href="http://www.unicode.org/versions/Unicode5.2.0/">Unicode 5.2</a></td></tr>
 * <tr><td>Android 4.0 (Ice Cream Sandwich)</td>
 *     <td>ICU 4.6</td>
 *     <td><a href="http://cldr.unicode.org/index/downloads/cldr-1-9">CLDR 1.9</a></td>
 *     <td><a href="http://www.unicode.org/versions/Unicode6.0.0/">Unicode 6.0</a></td></tr>
 * <tr><td>Android 4.1 (Jelly Bean)</td>
 *     <td>ICU 4.8</td>
 *     <td><a href="http://cldr.unicode.org/index/downloads/cldr-2-0">CLDR 2.0</a></td>
 *     <td><a href="http://www.unicode.org/versions/Unicode6.0.0/">Unicode 6.0</a></td></tr>
 * <tr><td>Android 4.3 (Jelly Bean MR2)</td>
 *     <td>ICU 50</td>
 *     <td><a href="http://cldr.unicode.org/index/downloads/cldr-22-1">CLDR 22.1</a></td>
 *     <td><a href="http://www.unicode.org/versions/Unicode6.2.0/">Unicode 6.2</a></td></tr>
 * <tr><td>Android 4.4 (KitKat)</td>
 *     <td>ICU 51</td>
 *     <td><a href="http://cldr.unicode.org/index/downloads/cldr-23">CLDR 23</a></td>
 *     <td><a href="http://www.unicode.org/versions/Unicode6.2.0/">Unicode 6.2</a></td></tr>
 * </table>
 *
 * <a name="default_locale"><h3>Be wary of the default locale</h3></a>
 * <p>Note that there are many convenience methods that automatically use the default locale, but
 * using them may lead to subtle bugs.
 *
 * <p>The default locale is appropriate for tasks that involve presenting data to the user. In
 * this case, you want to use the user's date/time formats, number
 * formats, rules for conversion to lowercase, and so on. In this case, it's safe to use the
 * convenience methods.
 *
 * <p>The default locale is <i>not</i> appropriate for machine-readable output. The best choice
 * there is usually {@code Locale.US}&nbsp;&ndash; this locale is guaranteed to be available on all
 * devices, and the fact that it has no surprising special cases and is frequently used (especially
 * for computer-computer communication) means that it tends to be the most efficient choice too.
 *
 * <p>A common mistake is to implicitly use the default locale when producing output meant to be
 * machine-readable. This tends to work on the developer's test devices (especially because so many
 * developers use en_US), but fails when run on a device whose user is in a more complex locale.
 *
 * <p>For example, if you're formatting integers some locales will use non-ASCII decimal
 * digits. As another example, if you're formatting floating-point numbers some locales will use
 * {@code ','} as the decimal point and {@code '.'} for digit grouping. That's correct for
 * human-readable output, but likely to cause problems if presented to another
 * computer ({@link Double#parseDouble} can't parse such a number, for example).
 * You should also be wary of the {@link String#toLowerCase} and
 * {@link String#toUpperCase} overloads that don't take a {@code Locale}: in Turkey, for example,
 * the characters {@code 'i'} and {@code 'I'} won't be converted to {@code 'I'} and {@code 'i'}.
 * This is the correct behavior for Turkish text (such as user input), but inappropriate for, say,
 * HTTP headers.
 */
public final class Locale implements Cloneable, Serializable {

    private static final long serialVersionUID = 9149081749638150636L;

    /**
     * Locale constant for en_CA.
     */
    public static final Locale CANADA = new Locale(true, "en", "CA");

    /**
     * Locale constant for fr_CA.
     */
    public static final Locale CANADA_FRENCH = new Locale(true, "fr", "CA");

    /**
     * Locale constant for zh_CN.
     */
    public static final Locale CHINA = new Locale(true, "zh", "CN");

    /**
     * Locale constant for zh.
     */
    public static final Locale CHINESE = new Locale(true, "zh", "");

    /**
     * Locale constant for en.
     */
    public static final Locale ENGLISH = new Locale(true, "en", "");

    /**
     * Locale constant for fr_FR.
     */
    public static final Locale FRANCE = new Locale(true, "fr", "FR");

    /**
     * Locale constant for fr.
     */
    public static final Locale FRENCH = new Locale(true, "fr", "");

    /**
     * Locale constant for de.
     */
    public static final Locale GERMAN = new Locale(true, "de", "");

    /**
     * Locale constant for de_DE.
     */
    public static final Locale GERMANY = new Locale(true, "de", "DE");

    /**
     * Locale constant for it.
     */
    public static final Locale ITALIAN = new Locale(true, "it", "");

    /**
     * Locale constant for it_IT.
     */
    public static final Locale ITALY = new Locale(true, "it", "IT");

    /**
     * Locale constant for ja_JP.
     */
    public static final Locale JAPAN = new Locale(true, "ja", "JP");

    /**
     * Locale constant for ja.
     */
    public static final Locale JAPANESE = new Locale(true, "ja", "");

    /**
     * Locale constant for ko_KR.
     */
    public static final Locale KOREA = new Locale(true, "ko", "KR");

    /**
     * Locale constant for ko.
     */
    public static final Locale KOREAN = new Locale(true, "ko", "");

    /**
     * Locale constant for zh_CN.
     */
    public static final Locale PRC = new Locale(true, "zh", "CN");

    /**
     * Locale constant for the root locale. The root locale has an empty language,
     * country, and variant.
     *
     * @since 1.6
     */
    public static final Locale ROOT = new Locale(true, "", "");

    /**
     * Locale constant for zh_CN.
     */
    public static final Locale SIMPLIFIED_CHINESE = new Locale(true, "zh", "CN");

    /**
     * Locale constant for zh_TW.
     */
    public static final Locale TAIWAN = new Locale(true, "zh", "TW");

    /**
     * Locale constant for zh_TW.
     */
    public static final Locale TRADITIONAL_CHINESE = new Locale(true, "zh", "TW");

    /**
     * Locale constant for en_GB.
     */
    public static final Locale UK = new Locale(true, "en", "GB");

    /**
     * Locale constant for en_US.
     */
    public static final Locale US = new Locale(true, "en", "US");

    /**
     * BCP-47 extension identifier (or "singleton") for the private
     * use extension.
     *
     * See {@link #getExtension(char)} and {@link Builder#setExtension(char, String)}.
     *
     * @hide
     * @since 1.7
     */
    public static final char PRIVATE_USE_EXTENSION = 'x';

    /**
     * BCP-47 extension identifier (or "singleton") for the unicode locale extension.
     *
     *
     * See {@link #getExtension(char)} and {@link Builder#setExtension(char, String)}.
     *
     * @hide
     * @since 1.7
     */
    public static final char UNICODE_LOCALE_EXTENSION = 'u';

    /**
     * The current default locale. It is temporarily assigned to US because we
     * need a default locale to lookup the real default locale.
     */
    private static Locale defaultLocale = US;

    static {
        String language = System.getProperty("user.language", "en");
        String region = System.getProperty("user.region", "US");
        String variant = System.getProperty("user.variant", "");
        defaultLocale = new Locale(language, region, variant);
    }

    /**
     * A class that helps construct {@link Locale} instances.
     *
     * Unlike the public {@code Locale} constructors, the methods of this class
     * perform much stricter checks on their input.
     *
     * Validity checks on the {@code language}, {@code country}, {@code variant}
     * and {@code extension} values are carried out as per the
     * <a href="https://tools.ietf.org/html/bcp47">BCP-47</a> specification.
     *
     * In addition, we treat the <a href="http://www.unicode.org/reports/tr35/">
     * Unicode locale extension</a> specially and provide methods to manipulate
     * the structured state (keywords and attributes) specified therein.
     *
     * @since 1.7
     * @hide
     */
    public static final class Builder {
        private String language;
        private String region;
        private String variant;
        private String script;

        private final Set<String> attributes;
        private final Map<String, String> keywords;
        private final Map<Character, String> extensions;

        public Builder() {
            language = region = variant = script = "";

            // NOTE: We use sorted maps in the builder & the locale class itself
            // because serialized forms of the unicode locale extension (and
            // of the extension map itself) are specified to be in alphabetic
            // order of keys.
            attributes = new TreeSet<String>();
            keywords = new TreeMap<String, String>();
            extensions = new TreeMap<Character, String>();
        }

        /**
         * Sets the locale language. If {@code language} is {@code null} or empty, the
         * previous value is cleared.
         *
         * As per BCP-47, the language must be between 2 and 3 ASCII characters
         * in length and must only contain characters in the range {@code [a-zA-Z]}.
         *
         * This value is usually an <a href="http://www.loc.gov/standards/iso639-2/">
         * ISO-639-2</a> alpha-2 or alpha-3 code, though no explicit checks are
         * carried out that it's a valid code in that namespace.
         *
         * Values are normalized to lower case.
         *
         * Note that we don't support BCP-47 "extlang" languages because they were
         * only ever used to substitute for a lack of 3 letter language codes.
         *
         * @throws IllformedLocaleException if the language was invalid.
         */
        public Builder setLanguage(String language) {
            if (language == null || language.isEmpty()) {
                this.language = "";
                return this;
            }

            final String lowercaseLanguage = language.toLowerCase(Locale.ROOT);
            if (!isValidBcp47Alpha(lowercaseLanguage, 2, 3)) {
                throw new IllformedLocaleException("Invalid language: " + language);
            }

            this.language = lowercaseLanguage;
            return this;
        }

        /**
         * Set the state of this builder to the parsed contents of the BCP-47 language
         * tag {@code languageTag}.
         *
         * This method is equivalent to a call to {@link #clear} if {@code languageTag}
         * is {@code null} or empty.
         *
         * <b>NOTE:</b> In contrast to {@link Locale#forLanguageTag(String)}, which
         * simply ignores malformed input, this method will throw an exception if
         * its input is malformed.
         *
         * @throws IllformedLocaleException if {@code languageTag} is not a well formed
         *         BCP-47 tag.
         */
        public Builder setLanguageTag(String languageTag) {
            if (languageTag == null || languageTag.isEmpty()) {
                clear();
                return this;
            }

            final Locale fromIcu = ICU.forLanguageTag(languageTag, true /* strict */);
            // When we ask ICU for strict parsing, it might return a null locale
            // if the language tag is malformed.
            if (fromIcu == null) {
                throw new IllformedLocaleException("Invalid languageTag: " + languageTag);
            }

            setLocale(fromIcu);
            return this;
        }

        /**
         * Sets the locale region. If {@code region} is {@code null} or empty, the
         * previous value is cleared.
         *
         * As per BCP-47, the region must either be a 2 character ISO-3166-1 code
         * (each character in the range [a-zA-Z]) OR a 3 digit UN M.49 code.
         *
         * Values are normalized to upper case.
         *
         * @throws IllformedLocaleException if {@code} region is invalid.
         */
        public Builder setRegion(String region) {
            if (region == null || region.isEmpty()) {
                this.region = "";
                return this;
            }

            final String uppercaseRegion = region.toUpperCase(Locale.ROOT);
            if (!isValidBcp47Alpha(uppercaseRegion, 2, 2) &&
                    !isUnM49AreaCode(uppercaseRegion)) {
                throw new IllformedLocaleException("Invalid region: " + region);
            }

            this.region = uppercaseRegion;
            return this;
        }

        /**
         * Sets the locale variant. If {@code variant} is {@code null} or empty,
         * the previous value is cleared.
         *
         * The input string my consist of one or more variants separated by
         * valid separators ('-' or '_').
         *
         * As per BCP-47, each variant must be between 5 and 8 alphanumeric characters
         * in length (each character in the range {@code [a-zA-Z0-9]}) but
         * can be exactly 4 characters in length if the first character is a digit.
         *
         * Note that this is a much stricter interpretation of {@code variant}
         * than the public {@code Locale} constructors. The latter allowed free form
         * variants.
         *
         * Variants are case sensitive and all separators are normalized to {@code '_'}.
         *
         * @throws IllformedLocaleException if {@code} variant is invalid.
         */
        public Builder setVariant(String variant) {
            if (variant == null || variant.isEmpty()) {
                this.variant = "";
                return this;
            }

            // Note that unlike extensions, we canonicalize to lower case alphabets
            // and underscores instead of hyphens.
            final String normalizedVariant = variant.replace('-', '_');
            String[] subTags = normalizedVariant.split("_");

            for (String subTag : subTags) {
                // The BCP-47 spec states that :
                // - Subtags can be between [5, 8] alphanumeric chars in length.
                // - Subtags that start with a number are allowed to be 4 chars in length.
                if (subTag.length() >= 5 && subTag.length() <= 8) {
                    if (!isAsciiAlphaNum(subTag)) {
                        throw new IllformedLocaleException("Invalid variant: " + variant);
                    }
                } else if (subTag.length() == 4) {
                    final char firstChar = subTag.charAt(0);
                    if (!(firstChar >= '0' && firstChar <= '9') || !isAsciiAlphaNum(subTag)) {
                        throw new IllformedLocaleException("Invalid variant: " + variant);
                    }
                } else {
                    throw new IllformedLocaleException("Invalid variant: " + variant);
                }
            }


            this.variant = normalizedVariant;
            return this;
        }

        /**
         * Sets the locale script. If {@code script} is {@code null} or empty,
         * the previous value is cleared.
         *
         * As per BCP-47, the script must be 4 characters in length, and
         * each character in the range {@code [a-zA-Z]}.
         *
         * A script usually represents a valid ISO 15924 script code, though no
         * other registry or validity checks are performed.
         *
         * Scripts are normalized to title cased values.
         *
         * @throws IllformedLocaleException if {@code script} is invalid.
         */
        public Builder setScript(String script) {
            if (script == null || script.isEmpty()) {
                this.script = "";
                return this;
            }

            if (!isValidBcp47Alpha(script, 4, 4)) {
                throw new IllformedLocaleException("Invalid script: " + script);
            }

            this.script = titleCaseAsciiWord(script);
            return this;
        }

        /**
         * Sets the state of the builder to the {@link Locale} represented by
         * {@code locale}.
         *
         * Note that the locale's language, region and variant are validated as per
         * the rules specified in {@link #setLanguage}, {@link #setRegion} and
         * {@link #setVariant}.
         *
         * All existing builder state is discarded.
         *
         * @throws IllformedLocaleException if {@code locale} is invalid.
         * @throws NullPointerException if {@code locale} is null.
         */
        public Builder setLocale(Locale locale) {
            if (locale == null) {
                throw new NullPointerException("locale == null");
            }

            // Make copies of the existing values so that we don't partially
            // update the state if we encounter an error.
            final String backupLanguage = language;
            final String backupRegion = region;
            final String backupVariant = variant;

            try {
                setLanguage(locale.getLanguage());
                setRegion(locale.getCountry());
                setVariant(locale.getVariant());
            } catch (IllformedLocaleException ifle) {
                language = backupLanguage;
                region = backupRegion;
                variant = backupVariant;

                throw ifle;
            }

            // The following values can be set only via the builder class, so
            // there's no need to normalize them or check their validity.

            this.script = locale.getScript();

            extensions.clear();
            extensions.putAll(locale.extensions);

            keywords.clear();
            keywords.putAll(locale.unicodeKeywords);

            attributes.clear();
            attributes.addAll(locale.unicodeAttributes);

            return this;
        }

        /**
         * Adds the specified attribute to the list of attributes in the unicode
         * locale extension.
         *
         * Attributes must be between 3 and 8 characters in length, and each character
         * must be in the range {@code [a-zA-Z0-9]}.
         *
         * Attributes are normalized to lower case values. All added attributes and
         * keywords are combined to form a complete unicode locale extension on
         * {@link Locale} objects built by this builder, and accessible via
         * {@link Locale#getExtension(char)} with the {@link Locale#UNICODE_LOCALE_EXTENSION}
         * key.
         *
         * @throws IllformedLocaleException if {@code attribute} is invalid.
         * @throws NullPointerException if {@code attribute} is null.
         */
        public Builder addUnicodeLocaleAttribute(String attribute) {
            if (attribute == null) {
                throw new NullPointerException("attribute == null");
            }

            final String lowercaseAttribute = attribute.toLowerCase(Locale.ROOT);
            if (!isValidBcp47Alphanum(lowercaseAttribute, 3, 8)) {
                throw new IllformedLocaleException("Invalid locale attribute: " + attribute);
            }

            attributes.add(lowercaseAttribute);

            return this;
        }

        /**
         * Removes an attribute from the list of attributes in the unicode locale
         * extension.
         *
         * {@code attribute} must be valid as per the rules specified in
         * {@link #addUnicodeLocaleAttribute}.
         *
         * This method has no effect if {@code attribute} hasn't already been
         * added.
         *
         * @throws IllformedLocaleException if {@code attribute} is invalid.
         * @throws NullPointerException if {@code attribute} is null.
         */
        public Builder removeUnicodeLocaleAttribute(String attribute) {
            if (attribute == null) {
                throw new NullPointerException("attribute == null");
            }

            // Weirdly, remove is specified to check whether the attribute
            // is valid, so we have to perform the full alphanumeric check here.
            final String lowercaseAttribute = attribute.toLowerCase(Locale.ROOT);
            if (!isValidBcp47Alphanum(lowercaseAttribute, 3, 8)) {
                throw new IllformedLocaleException("Invalid locale attribute: " + attribute);
            }

            attributes.remove(attribute);
            return this;
        }

        /**
         * Sets the extension identified by {@code key} to {@code value}.
         *
         * {@code key} must be in the range {@code [a-zA-Z0-9]}.
         *
         * If {@code value} is {@code null} or empty, the extension is removed.
         *
         * In the general case, {@code value} must be a series of subtags separated
         * by ({@code "-"} or {@code "_"}). Each subtag must be between
         * 2 and 8 characters in length, and each character in the subtag must be in
         * the range {@code [a-zA-Z0-9]}.
         *
         * <p>
         * There are two special cases :
         * <li>
         *     <ul>
         *         The unicode locale extension
         *         ({@code key == 'u'}, {@link Locale#UNICODE_LOCALE_EXTENSION}) : Setting
         *         the unicode locale extension results in all existing keyword and attribute
         *         state being replaced by the parsed result of {@code value}. For example,
         *         {@code  builder.setExtension('u', "baaaz-baaar-fo-baar-ba-baaz")}
         *         is equivalent to:
         *         <pre>
         *             builder.addUnicodeLocaleAttribute("baaaz");
         *             builder.addUnicodeLocaleAttribute("baaar");
         *             builder.setUnicodeLocaleKeyword("fo", "baar");
         *             builder.setUnicodeLocaleKeyword("ba", "baaa");
         *         </pre>
         *     </ul>
         *     <ul>
         *         The private use extension
         *         ({@code key == 'x'}, {@link Locale#PRIVATE_USE_EXTENSION}) : Each subtag in a
         *         private use extension can be between 1 and 8 characters in length (in contrast
         *         to a minimum length of 2 for all other extensions).
         *     </ul>
         * </li>
         *
         * @throws IllformedLocaleException if {@code value} is invalid.
         */
        public Builder setExtension(char key, String value) {
            if (value == null || value.isEmpty()) {
                extensions.remove(key);
                return this;
            }

            final String normalizedValue = value.toLowerCase(Locale.ROOT).replace('_', '-');
            final String[] subtags = normalizedValue.split("-");

            // Lengths for subtags in the private use extension should be [1, 8] chars.
            // For all other extensions, they should be [2, 8] chars.
            //
            // http://www.rfc-editor.org/rfc/bcp/bcp47.txt
            final int minimumLength = (key == PRIVATE_USE_EXTENSION) ? 1 : 2;
            for (String subtag : subtags) {
                if (!isValidBcp47Alphanum(subtag, minimumLength, 8)) {
                    throw new IllformedLocaleException(
                            "Invalid private use extension : " + value);
                }
            }

            // We need to take special action in the case of unicode extensions,
            // since we claim to understand their keywords and attributes.
            if (key == UNICODE_LOCALE_EXTENSION) {
                // First clear existing attributes and keywords.
                extensions.clear();
                attributes.clear();

                parseUnicodeExtension(subtags, keywords, attributes);
            } else {
                extensions.put(key, normalizedValue);
            }

            return this;
        }

        /**
         * Clears all extensions from this builder. Note that this also implicitly
         * clears all state related to the unicode locale extension; all attributes
         * and keywords set by {@link #addUnicodeLocaleAttribute} and
         * {@link #setUnicodeLocaleKeyword} are cleared.
         */
        public Builder clearExtensions() {
            extensions.clear();
            attributes.clear();
            keywords.clear();
            return this;
        }

        /**
         * Adds a key / type pair to the list of unicode locale extension keys.
         *
         * {@code key} must be 2 characters in length, and each character must be
         * in the range {@code [a-zA-Z0-9]}.
         *
         * {#code type} can either be empty, or a series of one or more subtags
         * separated by a separator ({@code "-"} or {@code "_"}). Each subtag must
         * be between 3 and 8 characters in length and each character in the subtag
         * must be in the range {@code [a-zA-Z0-9]}.
         *
         * Note that the type is normalized to lower case, and all separators
         * are normalized to {@code "-"}. All added attributes and
         * keywords are combined to form a complete unicode locale extension on
         * {@link Locale} objects built by this builder, and accessible via
         * {@link Locale#getExtension(char)} with the {@link Locale#UNICODE_LOCALE_EXTENSION}
         * key.
         *
         * @throws IllformedLocaleException if {@code key} or {@code value} are
         *         invalid.
         */
        public Builder setUnicodeLocaleKeyword(String key, String type) {
            if (key == null) {
                throw new NullPointerException("key == null");
            }

            if (type == null && keywords != null) {
                keywords.remove(key);
                return this;
            }

            final String lowerCaseKey = key.toLowerCase(Locale.ROOT);
            // The key must be exactly two alphanumeric characters.
            if (lowerCaseKey.length() != 2 || !isAsciiAlphaNum(lowerCaseKey)) {
                throw new IllformedLocaleException("Invalid unicode locale keyword: " + key);
            }

            // The type can be one or more alphanumeric strings of length [3, 8] characters,
            // separated by a separator char, which is one of "_" or "-". Though the spec
            // doesn't require it, we normalize all "_" to "-" to make the rest of our
            // processing easier.
            final String lowerCaseType = type.toLowerCase(Locale.ROOT).replace("_", "-");
            if (!isValidTypeList(lowerCaseType)) {
                throw new IllformedLocaleException("Invalid unicode locale type: " + type);
            }

            // Everything checks out fine, add the <key, type> mapping to the list.
            keywords.put(lowerCaseKey, lowerCaseType);

            return this;
        }

        /**
         * Clears all existing state from this builder.
         */
        public Builder clear() {
            clearExtensions();
            language = region = variant = script = "";

            return this;
        }

        /**
         * Constructs a locale from the existing state of the builder. Note that this
         * method is guaranteed to succeed since field validity checks are performed
         * at the point of setting them.
         */
        public Locale build() {
            // NOTE: We need to make a copy of attributes, keywords and extensions
            // because the RI allows this builder to reused.
            return new Locale(language, region, variant, script,
                    attributes, keywords, extensions,
                    false /* from public constructor */);
        }
    }

    /**
     * Returns a locale for a given BCP-47 language tag. This method is more
     * lenient than {@link Builder#setLanguageTag}. For a given language tag, parsing
     * will proceed upto the first malformed subtag. All subsequent tags are discarded.
     *
     * @throws NullPointerException if {@code languageTag} is {@code null}.
     *
     * @hide
     * @since 1.7
     */
    public static Locale forLanguageTag(String languageTag) {
        if (languageTag == null) {
            throw new NullPointerException("languageTag == null");
        }

        return ICU.forLanguageTag(languageTag, false /* strict */);
    }

    private transient String countryCode;
    private transient String languageCode;
    private transient String variantCode;
    private transient String scriptCode;

    private transient String cachedToStringResult;
    private transient String cachedLanguageTag;
    private transient String cachedIcuLocaleId;

    /* Sorted, Unmodifiable */
    private transient Set<String> unicodeAttributes;
    /* Sorted, Unmodifiable */
    private transient Map<String, String> unicodeKeywords;
    /* Sorted, Unmodifiable */
    private transient Map<Character, String> extensions;

    /**
     * There's a circular dependency between toLowerCase/toUpperCase and
     * Locale.US. Work around this by avoiding these methods when constructing
     * the built-in locales.
     *
     * @param unused required for this constructor to have a unique signature
     */
    private Locale(boolean unused, String lowerCaseLanguageCode, String upperCaseCountryCode) {
        this.languageCode = lowerCaseLanguageCode;
        this.countryCode = upperCaseCountryCode;
        this.variantCode = "";
        this.scriptCode = "";

        this.unicodeAttributes = Collections.EMPTY_SET;
        this.unicodeKeywords = Collections.EMPTY_MAP;
        this.extensions = Collections.EMPTY_MAP;
    }

    /**
     * Constructs a new {@code Locale} using the specified language.
     */
    public Locale(String language) {
        this(language, "", "", "", Collections.EMPTY_SET, Collections.EMPTY_MAP,
                Collections.EMPTY_MAP, true /* from public constructor */);
    }

    /**
     * Constructs a new {@code Locale} using the specified language and country codes.
     */
    public Locale(String language, String country) {
        this(language, country, "", "", Collections.EMPTY_SET, Collections.EMPTY_MAP,
                Collections.EMPTY_MAP, true /* from public constructor */);
    }

    /**
     * Required by libcore.icu.ICU.
     *
     * @hide
     */
    public Locale(String language, String country, String variant, String scriptCode,
            /* nonnull */ Set<String> unicodeAttributes,
            /* nonnull */ Map<String, String> unicodeKeywords,
            /* nonnull */ Map<Character, String> extensions,
            boolean fromPublicConstructor) {
        if (language == null || country == null || variant == null) {
            throw new NullPointerException("language=" + language +
                    ",country=" + country +
                    ",variant=" + variant);
        }

        if (fromPublicConstructor) {
            if (language.isEmpty() && country.isEmpty()) {
                languageCode = "";
                countryCode = "";
                variantCode = variant;
            } else {
                languageCode = adjustLanguageCode(language);
                countryCode = country.toUpperCase(Locale.US);
                variantCode = variant;
            }
        } else {
            this.languageCode = adjustLanguageCode(language);
            this.countryCode = country;
            this.variantCode = variant;
        }

        this.scriptCode = scriptCode;

        if (fromPublicConstructor) {
            this.unicodeAttributes = unicodeAttributes;
            this.unicodeKeywords = unicodeKeywords;
            this.extensions = extensions;
        } else {
            Set<String> attribsCopy = new TreeSet<String>(unicodeAttributes);
            Map<String, String> keywordsCopy = new TreeMap<String, String>(
                    unicodeKeywords);
            Map<Character, String> extensionsCopy = new TreeMap<Character, String>(
                    extensions);

            // We need to transform the list of attributes & keywords set on the
            // builder to a unicode locale extension. i.e, if we have any keywords
            // or attributes set, Locale#getExtension('u') should return a well
            // formed extension.
            addUnicodeExtensionToExtensionsMap(attribsCopy, keywordsCopy,
                    extensionsCopy);

            this.unicodeAttributes = Collections.unmodifiableSet(attribsCopy);
            this.unicodeKeywords = Collections.unmodifiableMap(keywordsCopy);
            this.extensions = Collections.unmodifiableMap(extensionsCopy);
        }
    }

    /**
     * Constructs a new {@code Locale} using the specified language, country,
     * and variant codes.
     */
    public Locale(String language, String country, String variant) {
        this(language, country, variant, "", Collections.EMPTY_SET,
                Collections.EMPTY_MAP, Collections.EMPTY_MAP,
                true /* from public constructor */);
    }

    @Override public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns true if {@code object} is a locale with the same language,
     * country and variant.
     */
    @Override public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof Locale) {
            Locale o = (Locale) object;
            return languageCode.equals(o.languageCode)
                    && countryCode.equals(o.countryCode)
                    && variantCode.equals(o.variantCode)
                    && scriptCode.equals(o.scriptCode)
                    && extensions.equals(o.extensions);

        }
        return false;
    }

    /**
     * Returns the system's installed locales. This array always includes {@code
     * Locale.US}, and usually several others. Most locale-sensitive classes
     * offer their own {@code getAvailableLocales} method, which should be
     * preferred over this general purpose method.
     *
     * @see java.text.BreakIterator#getAvailableLocales()
     * @see java.text.Collator#getAvailableLocales()
     * @see java.text.DateFormat#getAvailableLocales()
     * @see java.text.DateFormatSymbols#getAvailableLocales()
     * @see java.text.DecimalFormatSymbols#getAvailableLocales()
     * @see java.text.NumberFormat#getAvailableLocales()
     * @see java.util.Calendar#getAvailableLocales()
     */
    public static Locale[] getAvailableLocales() {
        return ICU.getAvailableLocales();
    }

    /**
     * Returns the country code for this locale, or {@code ""} if this locale
     * doesn't correspond to a specific country.
     */
    public String getCountry() {
        return countryCode;
    }

    /**
     * Returns the user's preferred locale. This may have been overridden for
     * this process with {@link #setDefault}.
     *
     * <p>Since the user's locale changes dynamically, avoid caching this value.
     * Instead, use this method to look it up for each use.
     */
    public static Locale getDefault() {
        return defaultLocale;
    }

    /**
     * Equivalent to {@code getDisplayCountry(Locale.getDefault())}.
     */
    public final String getDisplayCountry() {
        return getDisplayCountry(getDefault());
    }

    /**
     * Returns the name of this locale's country, localized to {@code locale}.
     * Returns the empty string if this locale does not correspond to a specific
     * country.
     */
    public String getDisplayCountry(Locale locale) {
        if (countryCode.isEmpty()) {
            return "";
        }
        String result = ICU.getDisplayCountryNative(getIcuLocaleId(), locale.getIcuLocaleId());
        if (result == null) { // TODO: do we need to do this, or does ICU do it for us?
            result = ICU.getDisplayCountryNative(getIcuLocaleId(),
                    Locale.getDefault().getIcuLocaleId());
        }
        return result;
    }

    /**
     * Equivalent to {@code getDisplayLanguage(Locale.getDefault())}.
     */
    public final String getDisplayLanguage() {
        return getDisplayLanguage(getDefault());
    }

    /**
     * Returns the name of this locale's language, localized to {@code locale}.
     * If the language name is unknown, the language code is returned.
     */
    public String getDisplayLanguage(Locale locale) {
        if (languageCode.isEmpty()) {
            return "";
        }

        // http://b/8049507 --- frameworks/base should use fil_PH instead of tl_PH.
        // Until then, we're stuck covering their tracks, making it look like they're
        // using "fil" when they're not.
        String localeString = toString();
        if (languageCode.equals("tl")) {
            localeString = toNewString("fil", countryCode, variantCode, scriptCode,
                    extensions);
        }

        String result = ICU.getDisplayLanguageNative(localeString, locale.getIcuLocaleId());
        if (result == null) { // TODO: do we need to do this, or does ICU do it for us?
            result = ICU.getDisplayLanguageNative(localeString,
                    Locale.getDefault().getIcuLocaleId());
        }
        return result;
    }

    /**
     * Equivalent to {@code getDisplayName(Locale.getDefault())}.
     */
    public final String getDisplayName() {
        return getDisplayName(getDefault());
    }

    /**
     * Returns this locale's language name, country name, and variant, localized
     * to {@code locale}. The exact output form depends on whether this locale
     * corresponds to a specific language, country and variant.
     *
     * <p>For example:
     * <ul>
     * <li>{@code new Locale("en").getDisplayName(Locale.US)} -> {@code English}
     * <li>{@code new Locale("en", "US").getDisplayName(Locale.US)} -> {@code English (United States)}
     * <li>{@code new Locale("en", "US", "POSIX").getDisplayName(Locale.US)} -> {@code English (United States,Computer)}
     * <li>{@code new Locale("en").getDisplayName(Locale.FRANCE)} -> {@code anglais}
     * <li>{@code new Locale("en", "US").getDisplayName(Locale.FRANCE)} -> {@code anglais (États-Unis)}
     * <li>{@code new Locale("en", "US", "POSIX").getDisplayName(Locale.FRANCE)} -> {@code anglais (États-Unis,informatique)}.
     * </ul>
     */
    public String getDisplayName(Locale locale) {
        int count = 0;
        StringBuilder buffer = new StringBuilder();
        if (!languageCode.isEmpty()) {
            String displayLanguage = getDisplayLanguage(locale);
            buffer.append(displayLanguage.isEmpty() ? languageCode : displayLanguage);
            ++count;
        }
        if (!scriptCode.isEmpty()) {
            if (count == 1) {
                buffer.append(" (");
            }
            String displayScript = getDisplayScript(locale);
            buffer.append(displayScript.isEmpty() ? countryCode : displayScript);
            ++count;
        }
        if (!countryCode.isEmpty()) {
            if (count == 1) {
                buffer.append(" (");
            }
            String displayCountry = getDisplayCountry(locale);
            buffer.append(displayCountry.isEmpty() ? countryCode : displayCountry);
            ++count;
        }
        if (!variantCode.isEmpty()) {
            if (count == 1) {
                buffer.append(" (");
            } else if (count == 2) {
                buffer.append(",");
            }
            String displayVariant = getDisplayVariant(locale);
            buffer.append(displayVariant.isEmpty() ? variantCode : displayVariant);
            ++count;
        }
        if (count > 1) {
            buffer.append(")");
        }
        return buffer.toString();
    }

    /**
     * Returns the full variant name in the default {@code Locale} for the variant code of
     * this {@code Locale}. If there is no matching variant name, the variant code is
     * returned.
     */
    public final String getDisplayVariant() {
        return getDisplayVariant(getDefault());
    }

    /**
     * Returns the full variant name in the specified {@code Locale} for the variant code
     * of this {@code Locale}. If there is no matching variant name, the variant code is
     * returned.
     */
    public String getDisplayVariant(Locale locale) {
        if (variantCode.length() == 0) {
            return variantCode;
        }
        String result = ICU.getDisplayVariantNative(getIcuLocaleId(), locale.getIcuLocaleId());
        if (result == null) { // TODO: do we need to do this, or does ICU do it for us?
            result = ICU.getDisplayVariantNative(getIcuLocaleId(),
                    Locale.getDefault().getIcuLocaleId());
        }
        return result;
    }

    /**
     * Returns the three-letter ISO 3166 country code which corresponds to the country
     * code for this {@code Locale}.
     * @throws MissingResourceException if there's no 3-letter country code for this locale.
     */
    public String getISO3Country() {
        String code = ICU.getISO3CountryNative(getIcuLocaleId());
        if (!countryCode.isEmpty() && code.isEmpty()) {
            throw new MissingResourceException("No 3-letter country code for locale: " + this, "FormatData_" + this, "ShortCountry");
        }
        return code;
    }

    /**
     * Returns the three-letter ISO 639-2/T language code which corresponds to the language
     * code for this {@code Locale}.
     * @throws MissingResourceException if there's no 3-letter language code for this locale.
     */
    public String getISO3Language() {
        String code = ICU.getISO3LanguageNative(getIcuLocaleId());
        if (!languageCode.isEmpty() && code.isEmpty()) {
            throw new MissingResourceException("No 3-letter language code for locale: " + this, "FormatData_" + this, "ShortLanguage");
        }
        return code;
    }

    /**
     * Returns an array of strings containing all the two-letter ISO 3166 country codes that can be
     * used as the country code when constructing a {@code Locale}.
     */
    public static String[] getISOCountries() {
        return ICU.getISOCountries();
    }

    /**
     * Returns an array of strings containing all the two-letter ISO 639-1 language codes that can be
     * used as the language code when constructing a {@code Locale}.
     */
    public static String[] getISOLanguages() {
        return ICU.getISOLanguages();
    }

    /**
     * Returns the language code for this {@code Locale} or the empty string if no language
     * was set.
     */
    public String getLanguage() {
        return languageCode;
    }

    /**
     * Returns the variant code for this {@code Locale} or an empty {@code String} if no variant
     * was set.
     */
    public String getVariant() {
        return variantCode;
    }

    /**
     * Returns the script code for this {@code Locale} or an empty {@code String} if no script
     * was set.
     *
     * If set, the script code will be a title cased string of length 4, as per the ISO 15924
     * specification.
     *
     * @hide
     * @since 1.7
     */
    public String getScript() {
        return scriptCode;
    }

    /**
     * Equivalent to {@code getDisplayScript(Locale.getDefault()))}
     *
     * @hide
     * @since 1.7
     */
    public String getDisplayScript() {
        return getDisplayScript(getDefault());
    }

    /**
     * Returns the name of this locale's script code, localized to {@link Locale}. If the
     * script code is unknown, the return value of this method is the same as that of
     * {@link #getScript()}.
     *
     * @hide
     * @since 1.7
     */
    public String getDisplayScript(Locale locale) {
        if (scriptCode.isEmpty()) {
            return "";
        }

        String result = ICU.getDisplayScriptNative(getIcuLocaleId(), locale.getIcuLocaleId());
        if (result == null) { // TODO: do we need to do this, or does ICU do it for us?
            result = ICU.getDisplayScriptNative(getIcuLocaleId(),
                    Locale.getDefault().getIcuLocaleId());
        }

        return result;

    }

    /**
     * Returns a well formed BCP-47 language tag that identifies this locale.
     *
     * Note that this locale itself might consist of ill formed fields, since the
     * public {@code Locale} constructors do not perform validity checks to maintain
     * backwards compatibility. When this is the case, this method will either replace
     * ill formed fields with standard BCP-47 subtags (For eg. "und" (undetermined)
     * for invalid languages) or omit them altogether.
     *
     * Additionally, ill formed variants will result in the remainder of the tag
     * (both variants and extensions) being moved to the private use extension,
     * where they will appear after a subtag whose value is {@code "lvariant"}.
     *
     * It's also important to note that the BCP-47 tag is well formed in the sense
     * that it is unambiguously parsable into its specified components. We do not
     * require that any of the components are registered with the applicable registries.
     * For example, we do not require scripts to be a registered ISO 15924 scripts or
     * languages to appear in the ISO-639-2 code list.
     *
     * @hide
     * @since 1.7
     */
    public String toLanguageTag() {
        if (cachedLanguageTag == null) {
            cachedLanguageTag = ICU.toLanguageTag(this);
        }

        return cachedLanguageTag;
    }

    /**
     * Returns the set of BCP-47 extensions this locale contains.
     *
     * See <a href="https://tools.ietf.org/html/bcp47#section-2.1">
     *     the IETF BCP-47 specification</a> (Section 2.2.6) for details.
     *
     * @hide
     * @since 1.7
     */
    public Set<Character> getExtensionKeys() {
        return extensions.keySet();
    }

    /**
     * Returns the BCP-47 extension whose key is {@code extensionKey}, or {@code null}
     * if this locale does not contain the extension.
     *
     * Individual Keywords and attributes for the unicode
     * locale extension can be fetched using {@link #getUnicodeLocaleAttributes()},
     * {@link #getUnicodeLocaleKeys()}  and {@link #getUnicodeLocaleType}.
     *
     * @hide
     * @since 1.7
     */
    public String getExtension(char extensionKey) {
        return extensions.get(extensionKey);
    }

    /**
     * Returns the {@code type} for the specified unicode locale extension {@code key}.
     *
     * For more information about types and keywords, see {@link Builder#setUnicodeLocaleKeyword}
     * and <a href="http://www.unicode.org/reports/tr35/#BCP47">Unicode Technical Standard #35</a>
     *
     * @hide
     * @since 1.7
     */
    public String getUnicodeLocaleType(String keyWord) {
        return unicodeKeywords.get(keyWord);
    }

    /**
     * Returns the set of unicode locale extension attributes this locale contains.
     *
     * For more information about attributes, see {@link Builder#addUnicodeLocaleAttribute}
     * and <a href="http://www.unicode.org/reports/tr35/#BCP47">Unicode Technical Standard #35</a>
     *
     * @hide
     * @since 1.7
     */
    public Set<String> getUnicodeLocaleAttributes() {
        return unicodeAttributes;
    }

    /**
     * Returns the set of unicode locale extension keywords this locale contains.
     *
     * For more information about types and keywords, see {@link Builder#setUnicodeLocaleKeyword}
     * and <a href="http://www.unicode.org/reports/tr35/#BCP47">Unicode Technical Standard #35</a>
     *
     * @hide
     * @since 1.7
     */
    public Set<String> getUnicodeLocaleKeys() {
        return unicodeKeywords.keySet();
    }

    @Override
    public synchronized int hashCode() {
        return countryCode.hashCode()
                + languageCode.hashCode() + variantCode.hashCode()
                + scriptCode.hashCode() + extensions.hashCode();
    }

    /**
     * Overrides the default locale. This does not affect system configuration,
     * and attempts to override the system-provided default locale may
     * themselves be overridden by actual changes to the system configuration.
     * Code that calls this method is usually incorrect, and should be fixed by
     * passing the appropriate locale to each locale-sensitive method that's
     * called.
     */
    public synchronized static void setDefault(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        defaultLocale = locale;
    }

    /**
     * Returns the string representation of this {@code Locale}. It consists of the
     * language code, country code and variant separated by underscores.
     * If the language is missing the string begins
     * with an underscore. If the country is missing there are 2 underscores
     * between the language and the variant. The variant cannot stand alone
     * without a language and/or country code: in this case this method would
     * return the empty string.
     *
     * <p>Examples: "en", "en_US", "_US", "en__POSIX", "en_US_POSIX"
     */
    @Override
    public final String toString() {
        String result = cachedToStringResult;
        if (result == null) {
            result = cachedToStringResult = toNewString(languageCode, countryCode,
                    variantCode, scriptCode, extensions);
        }
        return result;
    }

    private String getIcuLocaleId() {
        if (cachedIcuLocaleId == null) {
            cachedIcuLocaleId = ICU.localeIdFromLocale(this);
        }

        return cachedIcuLocaleId;
    }

    private static String toNewString(String languageCode, String countryCode,
            String variantCode, String scriptCode, Map<Character, String> extensions) {
        // The string form of a locale that only has a variant is the empty string.
        if (languageCode.length() == 0 && countryCode.length() == 0) {
            return "";
        }

        // Otherwise, the output format is "ll_cc_variant", where language and country are always
        // two letters, but the variant is an arbitrary length. A size of 11 characters has room
        // for "en_US_POSIX", the largest "common" value. (In practice, the string form is almost
        // always 5 characters: "ll_cc".)
        StringBuilder result = new StringBuilder(11);
        result.append(languageCode);

        final boolean hasScriptOrExtensions = !scriptCode.isEmpty() ||
                !extensions.isEmpty();

        if (!countryCode.isEmpty() || !variantCode.isEmpty() || hasScriptOrExtensions) {
            result.append('_');
        }
        result.append(countryCode);
        if (!variantCode.isEmpty() || hasScriptOrExtensions) {
            result.append('_');
        }
        result.append(variantCode);

        if (hasScriptOrExtensions) {
            if (!variantCode.isEmpty()) {
                result.append('_');
            }

            // Note that this is notably different from the BCP-47 spec (for
            // backwards compatibility). We are forced to append a "#" before the script tag.
            // and also put the script code right at the end.
            result.append("#");
            if (!scriptCode.isEmpty() ) {
                result.append(scriptCode);
            }

            // Note the use of "-" instead of "_" before the extensions.
            if (!extensions.isEmpty()) {
                if (!scriptCode.isEmpty()) {
                    result.append('-');
                }
                result.append(serializeExtensions(extensions));
            }
        }

        return result.toString();
    }

    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("country", String.class),
        new ObjectStreamField("hashcode", int.class),
        new ObjectStreamField("language", String.class),
        new ObjectStreamField("variant", String.class),
        new ObjectStreamField("script", String.class),
        new ObjectStreamField("extensions", String.class),
    };

    private void writeObject(ObjectOutputStream stream) throws IOException {
        ObjectOutputStream.PutField fields = stream.putFields();
        fields.put("country", countryCode);
        fields.put("hashcode", -1);
        fields.put("language", languageCode);
        fields.put("variant", variantCode);
        fields.put("script", scriptCode);

        if (!extensions.isEmpty()) {
            fields.put("extensions", serializeExtensions(extensions));
        }

        stream.writeFields();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = stream.readFields();
        countryCode = (String) fields.get("country", "");
        languageCode = (String) fields.get("language", "");
        variantCode = (String) fields.get("variant", "");
        scriptCode = (String) fields.get("script", "");

        this.unicodeKeywords = Collections.EMPTY_MAP;
        this.unicodeAttributes = Collections.EMPTY_SET;
        this.extensions = Collections.EMPTY_MAP;

        String extensions = (String) fields.get("extensions", null);
        if (extensions != null) {
            readExtensions(extensions);
        }
    }

    private void readExtensions(String extensions) {
        Map<Character, String> extensionsMap = new TreeMap<Character, String>();
        parseSerializedExtensions(extensions, extensionsMap);
        this.extensions = Collections.unmodifiableMap(extensionsMap);

        if (extensionsMap.containsKey(UNICODE_LOCALE_EXTENSION)) {
            String unicodeExtension = extensionsMap.get(UNICODE_LOCALE_EXTENSION);
            String[] subTags = unicodeExtension.split("-");

            Map<String, String> unicodeKeywords = new TreeMap<String, String>();
            Set<String> unicodeAttributes = new TreeSet<String>();
            parseUnicodeExtension(subTags, unicodeKeywords, unicodeAttributes);

            this.unicodeKeywords = Collections.unmodifiableMap(unicodeKeywords);
            this.unicodeAttributes = Collections.unmodifiableSet(unicodeAttributes);
        }
    }

    /**
     * The serialized form for extensions is straightforward. It's simply
     * of the form key1-value1-key2-value2 where each value might in turn contain
     * multiple subtags separated by hyphens. Each key is guaranteed to be a single
     * character in length.
     *
     * This method assumes that {@code extensionsMap} is non-empty.
     *
     * Visible for testing.
     *
     * @hide
     */
    public static String serializeExtensions(Map<Character, String> extensionsMap) {
        Iterator<Map.Entry<Character, String>> entryIterator = extensionsMap.entrySet().iterator();
        StringBuilder sb = new StringBuilder(64);

        while (true) {
            final Map.Entry<Character, String> entry = entryIterator.next();
            sb.append(entry.getKey());
            sb.append('-');
            sb.append(entry.getValue());

            if (entryIterator.hasNext()) {
                sb.append('-');
            } else {
                break;
            }
        }

        return sb.toString();
    }

    /**
     * Visible for testing.
     *
     * @hide
     */
    public static void parseSerializedExtensions(String extString, Map<Character, String> outputMap) {
        // This probably isn't the most efficient approach, but it's the
        // most straightforward to code.
        //
        // Start by splitting the string on "-". We will then keep track of
        // where each of the extension keys (single characters) appear in the
        // original string and then use those indices to construct substrings
        // representing the values.
        final String[] subTags = extString.split("-");
        final int[] typeStartIndices = new int[subTags.length / 2];

        int length = 0;
        int count = 0;
        for (String subTag : subTags) {
            if (subTag.length() > 0) {
                // Account for the length of the "-" at the end of each subtag.
                length += (subTag.length() + 1);
            }

            if (subTag.length() == 1) {
                typeStartIndices[count++] = length;
            }
        }

        for (int i = 0; i < count; ++i) {
            final int valueStart = typeStartIndices[i];
            // Since the start Index points to the beginning of the next type
            // ....prev-k-next.....
            //            |_ here
            // (idx - 2) is the index of the next key
            // (idx - 3) is the (non inclusive) end of the previous type.
            final int valueEnd = (i == (count - 1)) ?
                    extString.length() : (typeStartIndices[i + 1] - 3);

            outputMap.put(extString.charAt(typeStartIndices[i] - 2),
                    extString.substring(valueStart, valueEnd));
        }
    }


    /**
     * A UN M.49 is a 3 digit numeric code.
     */
    private static boolean isUnM49AreaCode(String code) {
        if (code.length() != 3) {
            return false;
        }

        for (int i = 0; i < 3; ++i) {
            final char character = code.charAt(i);
            if (!(character >= '0' && character <= '9')) {
                return false;
            }
        }

        return true;
    }

    /*
     * Checks whether a given string is an ASCII alphanumeric string.
     */
    private static boolean isAsciiAlphaNum(String string) {
        for (int i = 0; i < string.length(); i++) {
            final char character = string.charAt(i);
            if (!(character >= 'a' && character <= 'z' ||
                    character >= 'A' && character <= 'Z' ||
                    character >= '0' && character <= '9')) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidBcp47Alpha(String string,
            int lowerBound, int upperBound) {
        final int length = string.length();
        if (length < lowerBound || length > upperBound) {
            return false;
        }

        for (int i = 0; i < length; ++i) {
            final char character = string.charAt(i);
            if (!(character >= 'a' && character <= 'z' ||
                    character >= 'A' && character <= 'Z')) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidBcp47Alphanum(String attributeOrType,
            int lowerBound, int upperBound) {
        if (attributeOrType.length() < lowerBound || attributeOrType.length() > upperBound) {
            return false;
        }

        return isAsciiAlphaNum(attributeOrType);
    }

    private static String titleCaseAsciiWord(String word) {
        try {
            byte[] chars = word.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
            chars[0] = (byte) ((int) chars[0] + 'A' - 'a');
            return new String(chars, StandardCharsets.US_ASCII);
        } catch (UnsupportedOperationException uoe) {
            throw new AssertionError(uoe);
        }
    }

    /**
     * A type list must contain one or more alphanumeric subtags whose lengths
     * are between 3 and 8.
     */
    private static boolean isValidTypeList(String lowerCaseTypeList) {
        final String[] splitList = lowerCaseTypeList.split("-");
        for (String type : splitList) {
            if (!isValidBcp47Alphanum(type, 3, 8)) {
                return false;
            }
        }

        return true;
    }

    private static void addUnicodeExtensionToExtensionsMap(
            Set<String> attributes, Map<String, String> keywords,
            Map<Character, String> extensions) {
        if (attributes.isEmpty() && keywords.isEmpty()) {
            return;
        }

        // Assume that the common case is a low number of keywords & attributes
        // (usually one or two).
        final StringBuilder sb = new StringBuilder(32);

        // All attributes must appear before keywords, in lexical order.
        if (!attributes.isEmpty()) {
            Iterator<String> attributesIterator = attributes.iterator();
            while (true) {
                sb.append(attributesIterator.next());
                if (attributesIterator.hasNext()) {
                    sb.append('-');
                } else {
                    break;
                }
            }
        }

        if (!keywords.isEmpty()) {
            if (!attributes.isEmpty()) {
                sb.append('-');
            }

            Iterator<Map.Entry<String, String>> keywordsIterator = keywords.entrySet().iterator();
            while (true) {
                final Map.Entry<String, String> keyWord = keywordsIterator.next();
                sb.append(keyWord.getKey());
                sb.append('-');
                sb.append(keyWord.getValue());
                if (keywordsIterator.hasNext()) {
                    sb.append('-');
                } else {
                    break;
                }
            }
        }

        extensions.put(UNICODE_LOCALE_EXTENSION, sb.toString());
    }

    /**
     * This extension is described by http://www.unicode.org/reports/tr35/#RFC5234
     * unicode_locale_extensions = sep "u" (1*(sep keyword) / 1*(sep attribute) *(sep keyword)).
     *
     * It must contain at least one keyword or attribute and attributes (if any)
     * must appear before keywords. Attributes can't appear after keywords because
     * they will be indistinguishable from a subtag of the keyword type.
     *
     * Visible for testing.
     *
     * @hide
     */
    public static void parseUnicodeExtension(String[] subtags,
            Map<String, String> keywords, Set<String> attributes)  {
        String lastKeyword = null;
        List<String> subtagsForKeyword = new ArrayList<String>();
        for (String subtag : subtags) {
            if (subtag.length() == 2) {
                if (subtagsForKeyword.size() > 0) {
                    keywords.put(lastKeyword, joinBcp47Subtags(subtagsForKeyword));
                    subtagsForKeyword.clear();
                }

                lastKeyword = subtag;
            } else if (subtag.length() > 2) {
                if (lastKeyword == null) {
                    attributes.add(subtag);
                } else {
                    subtagsForKeyword.add(subtag);
                }
            }
        }

        if (subtagsForKeyword.size() > 0) {
            keywords.put(lastKeyword, joinBcp47Subtags(subtagsForKeyword));
        }
    }

    /**
     * Joins a list of subtags into a BCP-47 tag using the standard separator
     * ("-").
     */
    private static String joinBcp47Subtags(List<String> strings) {
        final int size = strings.size();

        StringBuilder sb = new StringBuilder(strings.get(0).length());
        for (int i = 0; i < size; ++i) {
            sb.append(strings.get(i));
            if (i != size - 1) {
                sb.append('-');
            }
        }

        return sb.toString();
    }

    private static String adjustLanguageCode(String languageCode) {
        String adjusted = languageCode.toLowerCase(Locale.US);
        // Map new language codes to the obsolete language
        // codes so the correct resource bundles will be used.
        if (languageCode.equals("he")) {
            adjusted = "iw";
        } else if (languageCode.equals("id")) {
            adjusted = "in";
        } else if (languageCode.equals("yi")) {
            adjusted = "ji";
        }

        return adjusted;
    }
}
