/*
 * Copyright (C) 2013 The Android Open Source Project
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

package libcore.icu;

public class TransliteratorTest extends junit.framework.TestCase {
  public void testAll() throws Exception {
    for (String id : Transliterator.getAvailableIDs()) {
      System.err.println(id);
      Transliterator.transliterate(id, "hello");
    }
  }

  public void test_Unknown() throws Exception {
    try {
      Transliterator.transliterate("Unknown", "hello");
      fail();
    } catch (RuntimeException expected) {
    }
  }

  public void test_null_id() throws Exception {
    try {
      Transliterator.transliterate(null, "hello");
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void test_null_string() throws Exception {
    try {
      Transliterator.transliterate("Any-Upper", null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void test_Any_Upper() throws Exception {
    assertEquals("HELLO WORLD!", Transliterator.transliterate("Any-Upper", "HeLlO WoRlD!"));
    assertEquals("STRASSE", Transliterator.transliterate("Any-Upper", "Straße"));
  }

  public void test_Any_Lower() throws Exception {
    assertEquals("hello world!", Transliterator.transliterate("Any-Lower", "HeLlO WoRlD!"));
  }

  public void test_Greek_Latin() throws Exception {
    String greek = "Καλημέρα κόσμε!";

    // Transliterate Greek to Latin, then to plain ASCII.
    String latin = Transliterator.transliterate("Greek-Latin", greek);
    String ascii = Transliterator.transliterate("Latin-Ascii", latin);
    assertEquals("Kalēméra kósme!", latin);
    assertEquals("Kalemera kosme!", ascii);

    // Use alternative transliteration variants.
    assertEquals("Kaliméra kósme!", Transliterator.transliterate("Greek-Latin/BGN", greek));
    assertEquals("Kali̱méra kósme!", Transliterator.transliterate("Greek-Latin/UNGEGN", greek));
  }

  public void test_Han_Latin() throws Exception {
    assertEquals("hàn zì/hàn zì", Transliterator.transliterate("Han-Latin", "汉字/漢字"));

    assertEquals("chén", Transliterator.transliterate("Han-Latin", "\u6c88"));
    assertEquals("shěn", Transliterator.transliterate("Han-Latin", "\u700b"));
    assertEquals("jiǎ", Transliterator.transliterate("Han-Latin", "\u8d3e"));

    assertEquals("shěn", Transliterator.transliterate("Han-Latin/Names", "\u6c88"));
    assertEquals("shěn", Transliterator.transliterate("Han-Latin/Names", "\u700b"));
    assertEquals("jǐa", Transliterator.transliterate("Han-Latin/Names", "\u8d3e"));
  }
}
