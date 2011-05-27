/*
 * Copyright (C) 2011 The Android Open Source Project
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

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;

public class InetAddressTest extends junit.framework.TestCase {
    private static final byte[] LOOPBACK6_BYTES = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };

    private static Inet6Address loopback6() throws Exception {
        return (Inet6Address) InetAddress.getByAddress(LOOPBACK6_BYTES);
    }

    private static Inet6Address localhost6() throws Exception {
        return (Inet6Address) InetAddress.getByAddress("localhost", LOOPBACK6_BYTES);
    }

    public void test_parseNumericAddress() throws Exception {
        // Regular IPv4.
        assertEquals("/1.2.3.4", InetAddress.parseNumericAddress("1.2.3.4").toString());
        // Regular IPv6.
        assertEquals("/2001:4860:800d::68", InetAddress.parseNumericAddress("2001:4860:800d::68").toString());
        // Weird IPv4 special cases.
        assertEquals("/1.2.0.3", InetAddress.parseNumericAddress("1.2.3").toString());
        assertEquals("/1.0.0.2", InetAddress.parseNumericAddress("1.2").toString());
        assertEquals("/0.0.0.1", InetAddress.parseNumericAddress("1").toString());
        assertEquals("/0.0.4.210", InetAddress.parseNumericAddress("1234").toString());
        // Optional square brackets around IPv6 addresses, including mapped IPv4.
        assertEquals("/2001:4860:800d::68", InetAddress.parseNumericAddress("[2001:4860:800d::68]").toString());
        assertEquals("/127.0.0.1", InetAddress.parseNumericAddress("[::ffff:127.0.0.1]").toString());
        try {
            // Actual IPv4 addresses may not be surrounded by square brackets.
            assertEquals("/127.0.0.1", InetAddress.parseNumericAddress("[127.0.0.1]").toString());
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            // Almost numeric but invalid...
            InetAddress.parseNumericAddress("1.");
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            // Not even close to numeric...
            InetAddress.parseNumericAddress("www.google.com");
            fail();
        } catch (IllegalArgumentException expected) {
        }
        // Strange special cases, for compatibility with InetAddress.getByName.
        assertTrue(InetAddress.parseNumericAddress(null).isLoopbackAddress());
        assertTrue(InetAddress.parseNumericAddress("").isLoopbackAddress());
    }

    public void test_getLoopbackAddress() throws Exception {
        assertTrue(InetAddress.getLoopbackAddress().isLoopbackAddress());
    }

    public void test_0() throws Exception {
        // The RI special-cases "0" for legacy IPv4 applications.
        assertTrue(InetAddress.getByName("0").isAnyLocalAddress());
    }

    public void test_equals() throws Exception {
        InetAddress addr = InetAddress.getByName("239.191.255.255");
        assertTrue(addr.equals(addr));
        assertTrue(loopback6().equals(localhost6()));
        assertFalse(addr.equals(loopback6()));

        InetAddress addr3 = InetAddress.getByName("127.0.0");
        assertFalse(loopback6().equals(addr3));

        assertTrue(Inet4Address.LOOPBACK.equals(Inet4Address.LOOPBACK));
    }

    public void test_getHostAddress() throws Exception {
        assertEquals("::1", localhost6().getHostAddress());
        assertEquals("::1", InetAddress.getByName("::1").getHostAddress());

        assertEquals("127.0.0.1", Inet4Address.LOOPBACK.getHostAddress());

        InetAddress aAddr = InetAddress.getByName("224.0.0.0");
        assertEquals("224.0.0.0", aAddr.getHostAddress());

        aAddr = InetAddress.getByName("1");
        assertEquals("0.0.0.1", aAddr.getHostAddress());

        aAddr = InetAddress.getByName("1.1");
        assertEquals("1.0.0.1", aAddr.getHostAddress());

        aAddr = InetAddress.getByName("1.1.1");
        assertEquals("1.1.0.1", aAddr.getHostAddress());

        byte[] bAddr = {
            (byte) 0xFE, (byte) 0x80, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x02, (byte) 0x11, (byte) 0x25, (byte) 0xFF,
            (byte) 0xFE, (byte) 0xF8, (byte) 0x7C, (byte) 0xB2
        };
        aAddr = Inet6Address.getByAddress(bAddr);
        String aString = aAddr.getHostAddress();
        assertTrue(aString.equals("fe80:0:0:0:211:25ff:fef8:7cb2") || aString.equals("fe80::211:25ff:fef8:7cb2"));

        byte[] cAddr = {
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        };
        aAddr = Inet6Address.getByAddress(cAddr);
        assertEquals("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", aAddr.getHostAddress());

        byte[] dAddr = {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        };
        aAddr = Inet6Address.getByAddress(dAddr);
        aString = aAddr.getHostAddress();
        assertTrue(aString.equals("0:0:0:0:0:0:0:0") || aString.equals("::"));

        byte[] eAddr = {
            (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03,
            (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07,
            (byte) 0x08, (byte) 0x09, (byte) 0x0a, (byte) 0x0b,
            (byte) 0x0c, (byte) 0x0d, (byte) 0x0e, (byte) 0x0f
        };
        aAddr = Inet6Address.getByAddress(eAddr);
        assertEquals("1:203:405:607:809:a0b:c0d:e0f", aAddr.getHostAddress());

        byte[] fAddr = {
            (byte) 0x00, (byte) 0x10, (byte) 0x20, (byte) 0x30,
            (byte) 0x40, (byte) 0x50, (byte) 0x60, (byte) 0x70,
            (byte) 0x80, (byte) 0x90, (byte) 0xa0, (byte) 0xb0,
            (byte) 0xc0, (byte) 0xd0, (byte) 0xe0, (byte) 0xf0
        };
        aAddr = Inet6Address.getByAddress(fAddr);
        assertEquals("10:2030:4050:6070:8090:a0b0:c0d0:e0f0", aAddr.getHostAddress());
    }

    public void test_hashCode() throws Exception {
        InetAddress addr1 = InetAddress.getByName("1.1");
        InetAddress addr2 = InetAddress.getByName("1.1.1");
        assertFalse(addr1.hashCode() == addr2.hashCode());

        addr2 = InetAddress.getByName("1.0.0.1");
        assertTrue(addr1.hashCode() == addr2.hashCode());

        assertTrue(loopback6().hashCode() == localhost6().hashCode());
    }

    public void test_toString() throws Exception {
        String validIPAddresses[] = {
            "::1.2.3.4", "::", "::", "1::0", "1::",
            "::1", "0", /* jdk1.5 accepts 0 as valid */
            "FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF",
            "FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:255.255.255.255",
            "0:0:0:0:0:0:0:0", "0:0:0:0:0:0:0.0.0.0"
        };

        String [] resultStrings = {
            "/::1.2.3.4", "/::", "/::", "/1::", "/1::",
            "/::1",
            "/0.0.0.0", "/ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
            "/ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", "/::",
            "/::"
        };

        for(int i = 0; i < validIPAddresses.length; i++) {
            InetAddress ia = InetAddress.getByName(validIPAddresses[i]);
            String result = ia.toString();
            assertNotNull(result);
            assertEquals(resultStrings[i], result);
        }
    }
}
