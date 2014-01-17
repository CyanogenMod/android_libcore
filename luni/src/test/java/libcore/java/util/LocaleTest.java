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

package libcore.java.util;

import java.text.BreakIterator;
import java.text.Collator;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.MissingResourceException;

public class LocaleTest extends junit.framework.TestCase {
    // http://b/2611311; if there's no display language/country/variant, use the raw codes.
    public void test_getDisplayName_invalid() throws Exception {
        Locale invalid = new Locale("AaBbCc", "DdEeFf", "GgHhIi");

        assertEquals("aabbcc", invalid.getLanguage());
        assertEquals("DDEEFF", invalid.getCountry());
        assertEquals("GgHhIi", invalid.getVariant());

        // Android using icu4c < 49.2 returned empty strings for display language, country,
        // and variant, but a display name made up of the raw strings.
        // Newer releases return slightly different results, but no less unreasonable.
        assertEquals("aabbcc", invalid.getDisplayLanguage());
        assertEquals("", invalid.getDisplayCountry());
        assertEquals("DDEEFF_GGHHII", invalid.getDisplayVariant());
        assertEquals("aabbcc (DDEEFF,DDEEFF_GGHHII)", invalid.getDisplayName());
    }

    // http://b/2611311; if there's no display language/country/variant, use the raw codes.
    public void test_getDisplayName_unknown() throws Exception {
        Locale unknown = new Locale("xx", "YY", "Traditional");
        assertEquals("xx", unknown.getLanguage());
        assertEquals("YY", unknown.getCountry());
        assertEquals("Traditional", unknown.getVariant());

        assertEquals("xx", unknown.getDisplayLanguage());
        assertEquals("YY", unknown.getDisplayCountry());
        assertEquals("TRADITIONAL", unknown.getDisplayVariant());
        assertEquals("xx (YY,TRADITIONAL)", unknown.getDisplayName());
    }

    public void test_getDisplayName_easy() throws Exception {
        assertEquals("English", Locale.ENGLISH.getDisplayLanguage(Locale.ENGLISH));
        assertEquals("German", Locale.GERMAN.getDisplayLanguage(Locale.ENGLISH));
        assertEquals("Englisch", Locale.ENGLISH.getDisplayLanguage(Locale.GERMAN));
        assertEquals("Deutsch", Locale.GERMAN.getDisplayLanguage(Locale.GERMAN));
    }

    public void test_getDisplayCountry_8870289() throws Exception {
        assertEquals("Hong Kong", new Locale("", "HK").getDisplayCountry(Locale.US));
        assertEquals("Macau", new Locale("", "MO").getDisplayCountry(Locale.US));
        assertEquals("Palestine", new Locale("", "PS").getDisplayCountry(Locale.US));

        assertEquals("Cocos [Keeling] Islands", new Locale("", "CC").getDisplayCountry(Locale.US));
        assertEquals("Congo [DRC]", new Locale("", "CD").getDisplayCountry(Locale.US));
        assertEquals("Congo [Republic]", new Locale("", "CG").getDisplayCountry(Locale.US));
        assertEquals("Falkland Islands [Islas Malvinas]", new Locale("", "FK").getDisplayCountry(Locale.US));
        assertEquals("Macedonia [FYROM]", new Locale("", "MK").getDisplayCountry(Locale.US));
        assertEquals("Myanmar [Burma]", new Locale("", "MM").getDisplayCountry(Locale.US));
        assertEquals("Taiwan", new Locale("", "TW").getDisplayCountry(Locale.US));
    }

    public void test_tl() throws Exception {
        // In jb-mr1, we had a last-minute hack to always return "Filipino" because
        // icu4c 4.8 didn't have any localizations for fil. (http://b/7291355)
        Locale tl = new Locale("tl");
        Locale tl_PH = new Locale("tl", "PH");
        assertEquals("Filipino", tl.getDisplayLanguage(Locale.ENGLISH));
        assertEquals("Filipino", tl_PH.getDisplayLanguage(Locale.ENGLISH));
        assertEquals("Filipino", tl.getDisplayLanguage(tl));
        assertEquals("Filipino", tl_PH.getDisplayLanguage(tl_PH));

        // After the icu4c 4.9 upgrade, we could localize "fil" correctly, though we
        // needed another hack to supply "fil" instead of "tl" to icu4c. (http://b/8023288)
        Locale es_MX = new Locale("es", "MX");
        assertEquals("filipino", tl.getDisplayLanguage(es_MX));
        assertEquals("filipino", tl_PH.getDisplayLanguage(es_MX));
      }

    // http://b/3452611; Locale.getDisplayLanguage fails for the obsolete language codes.
    public void test_getDisplayName_obsolete() throws Exception {
        // he (new) -> iw (obsolete)
        assertObsolete("he", "iw", "עברית");
        // id (new) -> in (obsolete)
        assertObsolete("id", "in", "Bahasa Indonesia");
    }

    private static void assertObsolete(String newCode, String oldCode, String displayName) {
        // Either code should get you the same locale.
        Locale newLocale = new Locale(newCode);
        Locale oldLocale = new Locale(oldCode);
        assertEquals(newLocale, oldLocale);

        // No matter what code you used to create the locale, you should get the old code back.
        assertEquals(oldCode, newLocale.getLanguage());
        assertEquals(oldCode, oldLocale.getLanguage());

        // Check we get the right display name.
        assertEquals(displayName, newLocale.getDisplayLanguage(newLocale));
        assertEquals(displayName, oldLocale.getDisplayLanguage(newLocale));
        assertEquals(displayName, newLocale.getDisplayLanguage(oldLocale));
        assertEquals(displayName, oldLocale.getDisplayLanguage(oldLocale));

        // Check that none of the 'getAvailableLocales' methods are accidentally returning two
        // equal locales (because to ICU they're different, but we mangle one into the other).
        assertOnce(newLocale, BreakIterator.getAvailableLocales());
        assertOnce(newLocale, Calendar.getAvailableLocales());
        assertOnce(newLocale, Collator.getAvailableLocales());
        assertOnce(newLocale, DateFormat.getAvailableLocales());
        assertOnce(newLocale, DateFormatSymbols.getAvailableLocales());
        assertOnce(newLocale, NumberFormat.getAvailableLocales());
        assertOnce(newLocale, Locale.getAvailableLocales());
    }

    private static void assertOnce(Locale element, Locale[] array) {
        int count = 0;
        for (Locale l : array) {
            if (l.equals(element)) {
                ++count;
            }
        }
        assertEquals(1, count);
    }

    public void test_getISO3Country() {
        // Empty country code.
        assertEquals("", new Locale("en", "").getISO3Country());

        // Invalid country code.
        try {
            assertEquals("", new Locale("en", "XX").getISO3Country());
            fail();
        } catch (MissingResourceException expected) {
            assertEquals("FormatData_en_XX", expected.getClassName());
            assertEquals("ShortCountry", expected.getKey());
        }

        // Valid country code.
        assertEquals("CAN", new Locale("", "CA").getISO3Country());
        assertEquals("CAN", new Locale("en", "CA").getISO3Country());
        assertEquals("CAN", new Locale("xx", "CA").getISO3Country());
    }

    public void test_getISO3Language() {
        // Empty language code.
        assertEquals("", new Locale("", "US").getISO3Language());

        // Invalid language code.
        try {
            assertEquals("", new Locale("xx", "US").getISO3Language());
            fail();
        } catch (MissingResourceException expected) {
            assertEquals("FormatData_xx_US", expected.getClassName());
            assertEquals("ShortLanguage", expected.getKey());
        }

        // Valid language code.
        assertEquals("eng", new Locale("en", "").getISO3Language());
        assertEquals("eng", new Locale("en", "CA").getISO3Language());
        assertEquals("eng", new Locale("en", "XX").getISO3Language());
    }
    /*

    public void test_serializeExtensions() {
        Map<Character, String> extensions = new TreeMap<Character, String>();

        extensions.put('x', "fooo-baar-baaz");
        assertEquals("x-fooo-baar-baaz", Locale.serializeExtensions(extensions));

        extensions.put('y', "gaaa-caar-caaz");
        // Must show up in lexical order.
        assertEquals("x-fooo-baar-baaz-y-gaaa-caar-caaz",
                Locale.serializeExtensions(extensions));
    }

    public void test_parseSerializedExtensions() {
        Map<Character, String> extensions = new HashMap<Character, String>();

        Locale.parseSerializedExtensions("x-foo", extensions);
        assertEquals("foo", extensions.get('x'));

        extensions.clear();
        Locale.parseSerializedExtensions("x-foo-y-bar-z-baz", extensions);
        assertEquals("foo", extensions.get('x'));
        assertEquals("bar", extensions.get('y'));
        assertEquals("baz", extensions.get('z'));

        extensions.clear();
        Locale.parseSerializedExtensions("x-fooo-baar-baaz", extensions);
        assertEquals("fooo-baar-baaz", extensions.get('x'));

        extensions.clear();
        Locale.parseSerializedExtensions("x-fooo-baar-baaz-y-gaaa-caar-caaz", extensions);
        assertEquals("fooo-baar-baaz", extensions.get('x'));
        assertEquals("gaaa-caar-caaz", extensions.get('y'));
    }

    public void test_parseUnicodeExtension() {
        Map<String, String> keywords = new HashMap<String, String>();
        Set<String> attributes = new HashSet<String>();

        // Only attributes.
        Locale.parseUnicodeExtension("foooo".split("-"), keywords, attributes);
        assertTrue(attributes.contains("foooo"));
        assertTrue(keywords.isEmpty());

        attributes.clear();
        keywords.clear();
        Locale.parseUnicodeExtension("foooo-baa-baaabaaa".split("-"),
                keywords, attributes);
        assertTrue(attributes.contains("foooo"));
        assertTrue(attributes.contains("baa"));
        assertTrue(attributes.contains("baaabaaa"));
        assertTrue(keywords.isEmpty());

        // Only keywords
        attributes.clear();
        keywords.clear();
        Locale.parseUnicodeExtension("ko-koko".split("-"), keywords, attributes);
        assertTrue(attributes.isEmpty());
        assertEquals("koko", keywords.get("ko"));

        attributes.clear();
        keywords.clear();
        Locale.parseUnicodeExtension("ko-koko-kokoko".split("-"), keywords, attributes);
        assertTrue(attributes.isEmpty());
        assertEquals("koko-kokoko", keywords.get("ko"));

        attributes.clear();
        keywords.clear();
        Locale.parseUnicodeExtension("ko-koko-kokoko-ba-baba-bababa".split("-"),
                keywords, attributes);
        assertTrue(attributes.isEmpty());
        assertEquals("koko-kokoko", keywords.get("ko"));
        assertEquals("baba-bababa", keywords.get("ba"));

        // A mixture of attributes and keywords.
        attributes.clear();
        keywords.clear();
        Locale.parseUnicodeExtension("attri1-attri2-k1-type1-type1-k2-type2".split("-"),
                keywords, attributes);
        assertTrue(attributes.contains("attri1"));
        assertTrue(attributes.contains("attri2"));
        assertEquals("type1-type1", keywords.get("k1"));
        assertEquals("type2", keywords.get("k2"));
    }
    */

    public void test_Builder_setLanguage() {
        Locale.Builder b = new Locale.Builder();

        // Should normalize to lower case.
        b.setLanguage("EN");
        assertEquals("en", b.build().getLanguage());

        b = new Locale.Builder();

        // Too short.
        try {
            b.setLanguage("e");
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Too long
        try {
            b.setLanguage("engl");
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Contains non ASCII characters
        try {
            b.setLanguage("தமிழ்");
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Null or empty languages must clear state.
        b = new Locale.Builder();
        b.setLanguage("en");
        b.setLanguage(null);
        assertEquals("", b.build().getLanguage());

        b = new Locale.Builder();
        b.setLanguage("en");
        b.setLanguage("");
        assertEquals("", b.build().getLanguage());
    }

    public void test_Builder_setRegion() {
        Locale.Builder b = new Locale.Builder();

        // Should normalize to upper case.
        b.setRegion("us");
        assertEquals("US", b.build().getCountry());

        b = new Locale.Builder();

        // Too short.
        try {
            b.setRegion("e");
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Too long
        try {
            b.setRegion("USA");
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Contains non ASCII characters
        try {
            b.setLanguage("திழ்");
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Null or empty regions must clear state.
        b = new Locale.Builder();
        b.setRegion("US");
        b.setRegion(null);
        assertEquals("", b.build().getCountry());

        b = new Locale.Builder();
        b.setRegion("US");
        b.setRegion("");
        assertEquals("", b.build().getCountry());
    }

    public void test_Builder_setVariant() {
        Locale.Builder b = new Locale.Builder();

        // Should normalize "_" to "-"
        b = new Locale.Builder();
        b.setVariant("vArIaNt-VaRiAnT-VARIANT");
        assertEquals("vArIaNt_VaRiAnT_VARIANT", b.build().getVariant());

        b = new Locale.Builder();
        // Too short
        try {
            b.setVariant("shor");
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Too long
        try {
            b.setVariant("waytoolong");
            fail();
        } catch (IllformedLocaleException expected) {
        }

        try {
            b.setVariant("foooo-foooo-fo");
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Special case. Variants of length 4 are allowed when the first
        // character is a digit.
        b.setVariant("0ABC");
        assertEquals("0ABC", b.build().getVariant());

        b = new Locale.Builder();
        b.setVariant("variant");
        b.setVariant(null);
        assertEquals("", b.build().getVariant());

        b = new Locale.Builder();
        b.setVariant("variant");
        b.setVariant("");
        assertEquals("", b.build().getVariant());
    }

    public void test_Builder_setLocale() {
        // Default case.
        Locale.Builder b = new Locale.Builder();
        b.setLocale(Locale.US);
        assertEquals("en", b.build().getLanguage());
        assertEquals("US", b.build().getCountry());

        // Should throw when locale is malformed.
        // - Bad language
        Locale bad = new Locale("e", "US");
        b = new Locale.Builder();
        try {
            b.setLocale(bad);
            fail();
        } catch (IllformedLocaleException expected) {
        }
        // - Bad country
        bad = new Locale("en", "USA");
        try {
            b.setLocale(bad);
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // - Bad variant
        bad = new Locale("en", "US", "c");
        try {
            b.setLocale(bad);
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Test values are normalized as they should be
        b = new Locale.Builder();
        Locale good = new Locale("EN", "us", "variant-VARIANT");
        b.setLocale(good);
        Locale l = b.build();
        assertEquals("en", l.getLanguage());
        assertEquals("US", l.getCountry());
        assertEquals("variant_VARIANT", l.getVariant());

        // Test that none of the existing fields are messed with
        // if the locale update fails.
        b = new Locale.Builder();
        b.setLanguage("fr").setRegion("FR");

        try {
            b.setLocale(bad);
            fail();
        } catch (IllformedLocaleException expected) {
        }

        l = b.build();
        assertEquals("fr", l.getLanguage());
        assertEquals("FR", l.getCountry());
    }

    public void test_Builder_setScript() {
        Locale.Builder b = new Locale.Builder();

        // Should normalize variants to lower case.
        b.setScript("lAtN");
        assertEquals("Latn", b.build().getScript());

        b = new Locale.Builder();
        // Too short
        try {
            b.setScript("lat");
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Too long
        try {
            b.setScript("latin");
            fail();
        } catch (IllformedLocaleException expected) {
        }

        b = new Locale.Builder();
        b.setScript("Latn");
        b.setScript(null);
        assertEquals("", b.build().getScript());

        b = new Locale.Builder();
        b.setScript("Latn");
        b.setScript("");
        assertEquals("", b.build().getScript());
    }

    public void test_Builder_clear() {
        Locale.Builder b = new Locale.Builder();
        b.setLanguage("en").setScript("Latn").setRegion("US")
                .setVariant("POSIX").setExtension('g', "foo")
                .setUnicodeLocaleKeyword("fo", "baar")
                .addUnicodeLocaleAttribute("baaaaz");

        Locale l = b.clear().build();
        assertEquals("", l.getLanguage());
        assertEquals("", l.getCountry());
        assertEquals("", l.getVariant());
        assertEquals("", l.getScript());
        assertTrue(l.getExtensionKeys().isEmpty());
    }

    public void test_Builder_setExtension() {
        Locale.Builder b = new Locale.Builder();
        b.setExtension('g', "FO_ba-BR_bg");

        Locale l = b.build();
        assertEquals("fo-ba-br-bg", l.getExtension('g'));

        b = new Locale.Builder();

        // Too short
        try {
            b.setExtension('g', "fo-ba-br-x");
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Too long
        try {
            b.setExtension('g', "fo-ba-br-extension");
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Special case, the private use extension allows single char subtags.
        b.setExtension(Locale.PRIVATE_USE_EXTENSION, "fo-ba-br-m");
        l = b.build();
        assertEquals("fo-ba-br-m", l.getExtension('x'));

        // Special case, the unicode locale extension must be parsed into
        // its individual components. The correctness of the parse is tested
        // in test_parseUnicodeExtension.
        b.setExtension(Locale.UNICODE_LOCALE_EXTENSION, "foooo_BaaaR-BA_Baz-bI_BIZ");
        l = b.build();
        // Note that attributes and keywords are sorted alphabetically.
        assertEquals("baaar-foooo-ba-baz-bi-biz", l.getExtension('u'));

        assertTrue(l.getUnicodeLocaleAttributes().contains("foooo"));
        assertTrue(l.getUnicodeLocaleAttributes().contains("baaar"));
        assertEquals("baz", l.getUnicodeLocaleType("ba"));
        assertEquals("biz", l.getUnicodeLocaleType("bi"));
    }

    public void test_Builder_clearExtensions() {
        Locale.Builder b = new Locale.Builder();
        b.setExtension('g', "FO_ba-BR_bg");
        b.setExtension(Locale.PRIVATE_USE_EXTENSION, "fo-ba-br-m");
        b.clearExtensions();

        assertTrue(b.build().getExtensionKeys().isEmpty());
    }

    private static Locale fromLanguageTag(String languageTag, boolean useBuilder) {
        if (useBuilder) {
            return (new Locale.Builder().setLanguageTag(languageTag).build());
        } else {
            return Locale.forLanguageTag(languageTag);
        }
    }

    private void test_setLanguageTag_wellFormedsingleSubtag(boolean useBuilder) {
        Locale l = fromLanguageTag("en", useBuilder);
        assertEquals("en", l.getLanguage());

        l = fromLanguageTag("eng", useBuilder);
        assertEquals("eng", l.getLanguage());
    }

    private void test_setLanguageTag_twoWellFormedSubtags(boolean useBuilder) {
        Locale l =  fromLanguageTag("en-US", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("US", l.getCountry());

        l =  fromLanguageTag("eng-419", useBuilder);
        assertEquals("eng", l.getLanguage());
        assertEquals("419", l.getCountry());

        // IND is an invalid region code so ICU helpfully tries to parse it as
        // a 3 letter language code, even if it isn't a valid ISO-639-3 code
        // either.
        l =  fromLanguageTag("en-USB", useBuilder);
        assertEquals("usb", l.getLanguage());
        assertEquals("", l.getCountry());

        // Script tags shouldn't be mis-recognized as regions.
        l =  fromLanguageTag("en-Latn", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("", l.getCountry());
        assertEquals("Latn", l.getScript());

        // Neither should variant tags.
        l =  fromLanguageTag("en-POSIX", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("", l.getCountry());
        assertEquals("", l.getScript());
        assertEquals("POSIX", l.getVariant());
    }

    public void test_Builder_setLanguageTag_malformedTags() {
        try {
            fromLanguageTag("a", true);
            fail();
        } catch (IllformedLocaleException ifle) {
        }

        // Three subtags
        // lang-region-illformedvariant
        try {
            fromLanguageTag("en-US-BA", true);
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // lang-variant-illformedvariant
        try {
            fromLanguageTag("en-FOOOO-BA", true);
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Four or more sub tags
        try {
            fromLanguageTag("en-US-POSIX-P2", true);
            fail();
        } catch (IllformedLocaleException expected) {
        }

        try {
            fromLanguageTag("en-Latn-US-P2", true);
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Extensions
        // Ill-formed empty extension.
        try {
            fromLanguageTag("en-f-f", true);
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Ill-formed empty extension.
        try {
            fromLanguageTag("en-f", true);
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Ill-formed extension with long subtag.
        try {
            fromLanguageTag("en-f-fooobaaaz", true);
            fail();
        } catch (IllformedLocaleException expected) {
        }

        // Ill-formed extension key.
        try {
            fromLanguageTag("en-9-baa", true);
            fail();
        } catch (IllformedLocaleException expected) {
        }
    }

    private void test_setLanguageTag_threeWellFormedSubtags(boolean useBuilder) {
        // lang-region-variant
        Locale l = fromLanguageTag("en-US-FOOOO", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("US", l.getCountry());
        assertEquals("", l.getScript());
        assertEquals("FOOOO", l.getVariant());

        // lang-script-variant
        l = fromLanguageTag("en-Latn-FOOOO", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("", l.getCountry());
        assertEquals("Latn", l.getScript());
        assertEquals("FOOOO", l.getVariant());

        // lang-script-region
        l = fromLanguageTag("en-Latn-US", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("US", l.getCountry());
        assertEquals("Latn", l.getScript());
        assertEquals("", l.getVariant());

        // lang-variant-variant
        l = fromLanguageTag("en-FOOOO-BAAAR", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("", l.getCountry());
        assertEquals("", l.getScript());
        assertEquals("FOOOO_BAAAR", l.getVariant());
    }

    private void test_setLanguageTag_fourOrMoreWellFormedSubtags(boolean useBuilder) {
        // lang-script-region-variant.
        Locale l = fromLanguageTag("en-Latn-US-foooo", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("Latn", l.getScript());
        assertEquals("US", l.getCountry());
        assertEquals("FOOOO", l.getVariant());

        // Variant with multiple subtags.
        l = fromLanguageTag("en-Latn-US-foooo-gfffh", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("Latn", l.getScript());
        assertEquals("US", l.getCountry());
        assertEquals("FOOOO_GFFFH", l.getVariant());

        // Variant with 3 subtags. POSIX shouldn't be recognized
        // as a region or a script.
        l = fromLanguageTag("en-POSIX-P2003-P2004", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("", l.getScript());
        assertEquals("", l.getCountry());
        assertEquals("POSIX_P2003_P2004", l.getVariant());

        // lang-script-variant-variant.
        l = fromLanguageTag("en-Latn-POSIX-P2003", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("Latn", l.getScript());
        assertEquals("", l.getCountry());
        assertEquals("POSIX_P2003", l.getVariant());

        // lang-region-variant-variant
        l = fromLanguageTag("en-US-POSIX-P2003", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("", l.getScript());
        assertEquals("US", l.getCountry());
        assertEquals("POSIX_P2003", l.getVariant());
    }

    private void test_setLanguageTag_withWellFormedExtensions(boolean useBuilder) {
        Locale l = fromLanguageTag("en-Latn-GB-foooo-g-fo-bar-baaz", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("Latn", l.getScript());
        assertEquals("GB", l.getCountry());
        assertEquals("FOOOO_POSIX", l.getVariant());
        assertEquals("fo-bar-baaz", l.getExtension('g'));

        // Multiple extensions
        l = fromLanguageTag("en-Latn-US-foooo-g-fo-bar-h-go-gaz", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("Latn", l.getScript());
        assertEquals("US", l.getCountry());
        assertEquals("FOOOO_POSIX", l.getVariant());
        assertEquals("fo-bar", l.getExtension('g'));
        assertEquals("go-gaz", l.getExtension('h'));

        // Unicode locale extension.
        l = fromLanguageTag("en-Latn-US-foooo-u-koooo-fo-bar", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("Latn", l.getScript());
        assertEquals("US", l.getCountry());
        assertEquals("koooo-fo-bar", l.getExtension('u'));
        assertTrue(l.getUnicodeLocaleAttributes().contains("koooo"));
        assertEquals("bar", l.getUnicodeLocaleType("fo"));

        // Extensions without variants
        l = fromLanguageTag("en-Latn-US-f-fo", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("Latn", l.getScript());
        assertEquals("US", l.getCountry());
        assertEquals("fo", l.getExtension('f'));

        l = fromLanguageTag("en-Latn-f-fo", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("Latn", l.getScript());
        assertEquals("fo", l.getExtension('f'));

        l = fromLanguageTag("en-f-fo", useBuilder);
        assertEquals("en", l.getLanguage());
        assertEquals("", l.getScript());
        assertEquals("", l.getCountry());
        assertEquals("fo", l.getExtension('f'));
    }

    public void test_forLanguageTag() {
        test_setLanguageTag_wellFormedsingleSubtag(false);
        test_setLanguageTag_twoWellFormedSubtags(false);
        test_setLanguageTag_threeWellFormedSubtags(false);
        test_setLanguageTag_fourOrMoreWellFormedSubtags(false);
        test_setLanguageTag_withWellFormedExtensions(false);
    }

    public void test_Builder_setLanguageTag() {
        test_setLanguageTag_wellFormedsingleSubtag(true);
        test_setLanguageTag_twoWellFormedSubtags(true);
        test_setLanguageTag_threeWellFormedSubtags(true);
        test_setLanguageTag_fourOrMoreWellFormedSubtags(true);
        test_setLanguageTag_withWellFormedExtensions(true);
    }

    public void test_getDisplayScript() {
        Locale.Builder b = new Locale.Builder();
        b.setLanguage("en").setRegion("US").setScript("Latn");

        Locale l = b.build();
        assertEquals("Latin", l.getDisplayScript());
        assertEquals("Lateinisch", l.getDisplayScript(Locale.GERMAN));
        // Fallback for navajo, a language for which we don't have data.
        assertEquals("Latin", l.getDisplayScript(new Locale("nv", "US")));

        b= new Locale.Builder();
        b.setLanguage("en").setRegion("US").setScript("Fooo");

        // Will be equivalent to getScriptCode for scripts that aren't
        // registered with ISO-15429 (but are otherwise well formed).
        l = b.build();
        assertEquals("Fooo", l.getDisplayScript());
    }

    public void test_setLanguageTag_malformedTags() {
        Locale l = fromLanguageTag("a", false);
        assertEquals("", l.getLanguage());
        assertEquals("", l.getCountry());
        assertEquals("", l.getVariant());
        assertEquals("", l.getScript());

        l = fromLanguageTag("en-US-BA", false);
        assertEquals("en", l.getLanguage());
        assertEquals("US", l.getCountry());
        assertEquals("", l.getVariant());
        assertEquals("", l.getScript());

        l = fromLanguageTag("en-FOOOO-BA", false);
        assertEquals("en", l.getLanguage());
        assertEquals("", l.getCountry());
        assertEquals("FOOOO", l.getVariant());
        assertEquals("", l.getScript());

        l = fromLanguageTag("en-US-POSIX-P2", false);
        assertEquals("en", l.getLanguage());
        assertEquals("US", l.getCountry());
        assertEquals("POSIX", l.getVariant());
        assertEquals("", l.getScript());

        l = fromLanguageTag("en-Latn-US-P2", false);
        assertEquals("en", l.getLanguage());
        assertEquals("US", l.getCountry());
        assertEquals("Latn", l.getScript());

        l = fromLanguageTag("en-f-f", false);
        assertEquals("en", l.getLanguage());
        assertEquals("", l.getCountry());
        assertEquals("", l.getVariant());
        assertEquals("", l.getScript());

        l = fromLanguageTag("en-f", false);
        assertEquals("en", l.getLanguage());
        assertEquals("", l.getCountry());
        assertEquals("", l.getVariant());
        assertEquals("", l.getScript());

        l = fromLanguageTag("en-f-fooobaaaz", false);
        assertEquals("en", l.getLanguage());
        assertEquals("", l.getCountry());
        assertEquals("", l.getVariant());
        assertEquals("", l.getScript());

        l = fromLanguageTag("en-9-baa", false);
        assertEquals("en", l.getLanguage());
        assertEquals("", l.getCountry());
        assertEquals("", l.getVariant());
        assertEquals("", l.getScript());
    }

    public void test_Builder_unicodeAttributes() {
        // Adding and removing attributes
        Locale.Builder b = new Locale.Builder();
        b.setLanguage("en");

        // Well formed attribute.
        b.addUnicodeLocaleAttribute("foooo");

        try {
            b.addUnicodeLocaleAttribute("fo");
            fail();
        } catch (IllformedLocaleException ifle) {
        }

        try {
            b.removeUnicodeLocaleAttribute("fo");
            fail();
        } catch (IllformedLocaleException ifle) {
        }

        try {
            b.addUnicodeLocaleAttribute("greaterthaneightchars");
            fail();
        } catch (IllformedLocaleException ifle) {
        }

        try {
            b.removeUnicodeLocaleAttribute("greaterthaneightchars");
            fail();
        } catch (IllformedLocaleException ifle) {
        }

        try {
            b.addUnicodeLocaleAttribute(null);
            fail();
        } catch (NullPointerException npe) {
        }

        try {
            b.removeUnicodeLocaleAttribute(null);
            fail();
        } catch (NullPointerException npe) {
        }

        Locale l = b.build();
        assertEquals("en-u-foooo", l.toLanguageTag());
        assertTrue(l.getUnicodeLocaleAttributes().contains("foooo"));

        b.addUnicodeLocaleAttribute("dAtA");
        l = b.build();
        assertEquals("data-foooo", l.getExtension('u'));
        assertTrue(l.getUnicodeLocaleAttributes().contains("data"));
        assertTrue(l.getUnicodeLocaleAttributes().contains("foooo"));
    }

    public void test_Builder_unicodeKeywords() {
        // Adding and removing attributes
        Locale.Builder b = new Locale.Builder();
        b.setLanguage("en");

        // Key not of length 2.
        try {
            b.setUnicodeLocaleKeyword("k", "fooo");
            fail();
        } catch (IllformedLocaleException ifle) {
        }

        // Value too short
        try {
            b.setUnicodeLocaleKeyword("k", "fo");
            fail();
        } catch (IllformedLocaleException ifle) {
        }

        // Value too long
        try {
            b.setUnicodeLocaleKeyword("k", "foooooooo");
            fail();
        } catch (IllformedLocaleException ifle) {
        }


        // Null should clear the key.
        b.setUnicodeLocaleKeyword("bo", "baaz");
        Locale l = b.build();
        assertEquals("bo-baaz", l.getExtension('u'));
        assertEquals("baaz", l.getUnicodeLocaleType("bo"));

        b = new Locale.Builder();
        b.setUnicodeLocaleKeyword("bo", "baaz");
        b.setUnicodeLocaleKeyword("bo", null);
        l = b.build();
        assertNull(l.getExtension('u'));
        assertNull(l.getUnicodeLocaleType("bo"));

        // When we set attributes, they should show up before extensions.
        b = new Locale.Builder();
        b.addUnicodeLocaleAttribute("fooo");
        b.addUnicodeLocaleAttribute("gooo");
        b.setUnicodeLocaleKeyword("fo", "baz");
        b.setUnicodeLocaleKeyword("ka", "kaz");
        l = b.build();
        assertEquals("fooo-gooo-fo-baz-ka-kaz", l.getExtension('u'));
        assertEquals("baz", l.getUnicodeLocaleType("fo"));
        assertEquals("kaz", l.getUnicodeLocaleType("ka"));
        assertTrue(l.getUnicodeLocaleAttributes().contains("fooo"));
        assertTrue(l.getUnicodeLocaleAttributes().contains("gooo"));
    }

    public void test_multipleExtensions() {
        Locale.Builder b = new Locale.Builder();
        b.setLanguage("en");
        b.addUnicodeLocaleAttribute("attrib");
        b.addUnicodeLocaleAttribute("attrib2");
        b.setExtension('f', "fo-baaz-ga-gaaz");
        b.setExtension('x', "xo-baaz-ga-gaaz");
        b.setExtension('z', "zo-baaz-ga-gaaz");

        Locale l = b.build();
        // Implicitly added because we added unicode locale attributes.
        assertEquals("attrib-attrib2", l.getExtension('u'));
        assertEquals("fo-baaz-ga-gaaz", l.getExtension('f'));
        assertEquals("xo-baaz-ga-gaaz", l.getExtension('x'));
        assertEquals("zo-baaz-ga-gaaz", l.getExtension('z'));
    }

    public void test_immutability() {
        Locale.Builder b = new Locale.Builder();
        b.setExtension('g', "fooo-baaz-baar");
        b.setExtension('u', "foooo-baaar-ba-baaz-ka-kaaz");

        Locale l = b.build();
        try {
            l.getExtensionKeys().add('g');
            fail();
        } catch (UnsupportedOperationException expected) {
        }

        try {
            l.getUnicodeLocaleAttributes().add("fooo");
            fail();
        } catch (UnsupportedOperationException expected) {
        }
    }

    public void test_toLanguageTag() {
        Locale.Builder b = new Locale.Builder();

        // Empty builder.
        Locale l = b.build();
        // TODO: Fix this. We should return "und" and not NULL.
        // assertEquals("und", l.toLanguageTag());

        // Only language.
        b = new Locale.Builder();
        b.setLanguage("en");
        assertEquals("en", b.build().toLanguageTag());

        // Language & Region
        b = new Locale.Builder();
        b.setLanguage("en").setRegion("US");
        assertEquals("en-US", b.build().toLanguageTag());

        // Language & Script
        b = new Locale.Builder();
        b.setLanguage("en").setScript("Latn");
        assertEquals("en-Latn", b.build().toLanguageTag());

        // Language & Variant
        b = new Locale.Builder();
        b.setLanguage("en").setVariant("foooo");
        assertEquals("en-foooo", b.build().toLanguageTag());

        // Language / script & country
        b = new Locale.Builder();
        b.setLanguage("en").setScript("Latn").setRegion("US");
        assertEquals("en-Latn-US", b.build().toLanguageTag());

        // Language / script & variant
        b = new Locale.Builder();
        b.setLanguage("en").setScript("Latn").setVariant("foooo");
        assertEquals("en-Latn-foooo", b.build().toLanguageTag());

        // Language / script / country / variant.
        b = new Locale.Builder();
        b.setLanguage("en").setScript("Latn").setVariant("foooo").setRegion("US");
        assertEquals("en-Latn-US-foooo", b.build().toLanguageTag());

        // Language / extension
        b = new Locale.Builder();
        b.setLanguage("en").setExtension('x', "fooo-baar");
        assertEquals("en-x-fooo-baar", b.build().toLanguageTag());

        // Language & multiple extensions (including unicode).
        b = new Locale.Builder();
        b.setLanguage("en");
        b.addUnicodeLocaleAttribute("attrib");
        b.addUnicodeLocaleAttribute("attrib2");
        b.setExtension('f', "fo-baaz-ga-gaaz");
        b.setExtension('x', "xo-baaz-ga-gaaz");
        b.setExtension('z', "zo-baaz-ga-gaaz");

        l = b.build();
        // Implicitly added because we added unicode locale attributes.
        assertEquals("attrib-attrib2", l.getExtension('u'));
        assertEquals("fo-baaz-ga-gaaz", l.getExtension('f'));
        assertEquals("xo-baaz-ga-gaaz", l.getExtension('x'));
        assertEquals("zo-baaz-ga-gaaz", l.getExtension('z'));

        assertEquals("en-" +
                "f-fo-baaz-ga-gaaz-" +   // extension tags in lexical order
                "u-attrib-attrib2-z-zo-baaz-ga-gaaz-" +  // unicode attribs & keywords in lex order
                "x-xo-baaz-ga-gaaz", // private use extension unmodified.
                l.toLanguageTag());
    }

    public void test_toString() {
                Locale.Builder b = new Locale.Builder();

        // Empty builder.
        Locale l = b.build();
        assertEquals("", l.toString());

        // Only language.
        b = new Locale.Builder();
        b.setLanguage("en");
        assertEquals("en", b.build().toString());

        // Only region
        b = new Locale.Builder();
        b.setRegion("US");
        assertEquals("_US", b.build().toString());

        // Language & Region
        b = new Locale.Builder();
        b.setLanguage("en").setRegion("US");
        assertEquals("en_US", b.build().toString());

        // Language & Script
        b = new Locale.Builder();
        b.setLanguage("en").setScript("Latn");
        assertEquals("en__#Latn", b.build().toString());

        // Language & Variant
        b = new Locale.Builder();
        b.setLanguage("en").setVariant("foooo");
        assertEquals("en__foooo", b.build().toString());

        // Language / script & country
        b = new Locale.Builder();
        b.setLanguage("en").setScript("Latn").setRegion("US");
        assertEquals("en_US_#Latn", b.build().toString());

        // Language / script & variant
        b = new Locale.Builder();
        b.setLanguage("en").setScript("Latn").setVariant("foooo");
        assertEquals("en__foooo_#Latn", b.build().toString());

        // Language / script / country / variant.
        b = new Locale.Builder();
        b.setLanguage("en").setScript("Latn").setVariant("foooo").setRegion("US");
        assertEquals("en_US_foooo_#Latn", b.build().toString());

        // Language / extension
        b = new Locale.Builder();
        b.setLanguage("en").setExtension('x', "fooo-baar");
        assertEquals("en__#x-fooo-baar", b.build().toString());
    }
}

