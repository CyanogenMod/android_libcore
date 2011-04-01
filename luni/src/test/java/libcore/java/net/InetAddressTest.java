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

public class InetAddressTest extends junit.framework.TestCase {
    public void test_parseNumericAddress() throws Exception {
        // Regular IPv4.
        assertEquals("/1.2.3.4", InetAddress.parseNumericAddress("1.2.3.4").toString());
        // Regular IPv6.
        assertEquals("/2001:4860:800d::68", InetAddress.parseNumericAddress("2001:4860:800d::68").toString());
        // Weird IPv4 special cases.
        assertEquals("/1.2.0.3", InetAddress.parseNumericAddress("1.2.3").toString());
        assertEquals("/1.0.0.2", InetAddress.parseNumericAddress("1.2").toString());
        assertEquals("/0.0.0.1", InetAddress.parseNumericAddress("1").toString());
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
}
