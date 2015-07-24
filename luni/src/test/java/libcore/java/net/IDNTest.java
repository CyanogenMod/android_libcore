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

package libcore.java.net;

import java.net.IDN;
import junit.framework.TestCase;

public class IDNTest extends TestCase {

    public void test_toAscii() {
        assertEquals("fass.de", IDN.toASCII("fass.de"));
        assertEquals("fass.de", IDN.toASCII("faß.de"));
        assertEquals("xn--fss-qla.de", IDN.toASCII("fäß.de"));
        assertEquals("xn--yzg.com", IDN.toASCII("₹.com"));
        assertEquals("xn--n00d.com", IDN.toASCII("\uD804\uDC13.com"));
        assertEquals("ab", IDN.toASCII("a\u200Cb"));
        assertEquals("xn--bb-eka.at", IDN.toASCII("öbb.at"));
        assertEquals("xn--og-09a.de", IDN.toASCII("ȡog.de"));
        assertEquals("xn--53h.de", IDN.toASCII("☕.de"));
        assertEquals("xn--iny-zx5a.de", IDN.toASCII("i♥ny.de"));
        assertEquals("xn--abc-rs4b422ycvb.co.jp", IDN.toASCII("abc・日本.co.jp"));
        assertEquals("xn--wgv71a.co.jp", IDN.toASCII("日本.co.jp"));
        assertEquals("xn--x-xbb7i.de", IDN.toASCII("x\u0327\u0301.de"));
        assertEquals("xn--wxaikc6b.gr", IDN.toASCII("σόλοσ.gr"));
        assertEquals("xn--wxaikc6b.xn--gr-gtd9a1b0g.de", IDN.toASCII("σόλοσ.grعربي.de"));
    }

    public void test_toUnicode() {
        assertEquals("fäss.de", IDN.toUnicode("xn--fss-qla.de"));
        assertEquals("₹.com", IDN.toUnicode("xn--yzg.com"));
        assertEquals("\uD804\uDC13.com", IDN.toUnicode("xn--n00d.com"));
        assertEquals("öbb.at", IDN.toUnicode("xn--bb-eka.at"));
        assertEquals("ȡog.de", IDN.toUnicode("xn--og-09a.de"));
        assertEquals("☕.de", IDN.toUnicode("xn--53h.de"));
        assertEquals("i♥ny.de", IDN.toUnicode("xn--iny-zx5a.de"));
        assertEquals("abc・日本.co.jp", IDN.toUnicode("xn--abc-rs4b422ycvb.co.jp"));
        assertEquals("日本.co.jp", IDN.toUnicode("xn--wgv71a.co.jp"));
        assertEquals("x\u0327\u0301.de", IDN.toUnicode("xn--x-xbb7i.de"));
        assertEquals("σόλοσ.gr", IDN.toUnicode("xn--wxaikc6b.gr"));
        assertEquals("σόλοσ.grعربي.de", IDN.toUnicode("xn--wxaikc6b.xn--gr-gtd9a1b0g.de"));
    }

    public void testFullstops() {
        assertEquals("example.com", IDN.toASCII("example。com"));
        assertEquals("example.com", IDN.toASCII("example．com"));
        assertEquals("example.com", IDN.toASCII("example｡com"));
    }


    public void test_invalid() {
        // Ⅎ is a dissalowed character under IDNA2008 and UTS46.
        try {
            IDN.toASCII("Ⅎ.net");
            fail();
        } catch (IllegalArgumentException expected) {
        }
        // U+2F868 (a CJK compatibility ideograph) is a disallowed
        // character under IDNA2008 and UTS46.
        try {
            IDN.toASCII("\uD87E\uDC68.net");
            fail();
        } catch (IllegalArgumentException expected) {
        }
        assertEquals("Ⅎ.net", IDN.toUnicode("Ⅎ.net"));
        assertEquals("\uD87E\uDC68.net", IDN.toUnicode("\uD87E\uDC68.net"));
    }

    private static String makePunyString(int xCount) {
        StringBuilder s = new StringBuilder();
        s.append("xn--bcher");
        for (int i = 0; i < xCount; ++i) {
            s.append('x');
        }
        s.append("-kva");
        return s.toString();
    }

    public void test_toUnicode_failures() {
        // This is short enough to work...
        assertEquals("b\u00fccher", IDN.toUnicode(makePunyString(0)));
        // This is too long, and the RI just returns the input string...
        String longInput = makePunyString(512);
        assertEquals(longInput, IDN.toUnicode(longInput));
    }
}
