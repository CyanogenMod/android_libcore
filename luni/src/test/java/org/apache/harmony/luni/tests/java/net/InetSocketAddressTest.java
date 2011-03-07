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
package org.apache.harmony.luni.tests.java.net;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.Permission;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

public class InetSocketAddressTest extends TestCase {

    /**
     * java.net.InetSocketAddress#InetSocketAddress(String, int)
     */
    public void test_ConstructorLjava_lang_StringI() throws Exception {
        // regression test for Harmony-1042
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
        assertNotNull(address.getHostName());

        try {
            new InetSocketAddress("127.0.0.1", -1);
            fail("IllegalArgumentException was not thrown.");
        } catch(IllegalArgumentException iae) {
            //expected
        }

        try {
            new InetSocketAddress("127.0.0.1", 65536);
            fail("IllegalArgumentException was not thrown.");
        } catch(IllegalArgumentException iae) {
            //expected
        }
    }

    public void test_ConstructorLInetAddressI() {
        String validIPAddresses[] = { "::1.2.3.4", "::", "::", "1::0", "1::",
                "::1", "0",
                "FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF",
                "FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:255.255.255.255",
                "0:0:0:0:0:0:0:0", "0:0:0:0:0:0:0.0.0.0",
                "127.0.0.1", "localhost", "1.1", "42.42.42.42", "0.0.0.0"};

        String results [] = { "0:0:0:0:0:0:102:304", "0:0:0:0:0:0:0:0",
                "0:0:0:0:0:0:0:0", "1:0:0:0:0:0:0:0", "1:0:0:0:0:0:0:0",
                "0:0:0:0:0:0:0:1", "0.0.0.0",
                "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
                "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
                "0:0:0:0:0:0:0:0", "0:0:0:0:0:0:0:0",
                "localhost", "localhost", "1.0.0.1", "42.42.42.42", "0.0.0.0"};

        for(int i = 0; i < validIPAddresses.length; i++) {
            try {
                InetAddress ia = InetAddress.getByName(validIPAddresses[i]);
                InetSocketAddress isa = new InetSocketAddress(ia, 80);
                assertEquals(80,isa.getPort());
                //assertEquals(results[i], isa.getHostName());
            } catch(UnknownHostException uhe) {
                fail("UnknownHostException was thrown for: " +
                        validIPAddresses[i]);
            }
        }
        try {
            InetSocketAddress isa = new InetSocketAddress((InetAddress)null, 80);
            assertEquals("0.0.0.0", isa.getHostName());
        } catch(Exception e) {
            fail("Unexpected exception was thrown.");
        }

        try {
            InetAddress isa = InetAddress.getByName("localhost");
            new InetSocketAddress(isa, 65536);
            fail("IllegalArgumentException was not thrown.");
        } catch(IllegalArgumentException iae) {
            //expected
        } catch (UnknownHostException e) {
            fail("UnknownHostException was thrown.");
        }

        try {
            InetAddress isa = InetAddress.getByName("localhost");
            new InetSocketAddress(isa, -1);
            fail("IllegalArgumentException was not thrown.");
        } catch(IllegalArgumentException iae) {
            //expected
        } catch (UnknownHostException e) {
            fail("UnknownHostException was thrown.");
        }
    }
    public void test_ConstructorI() {

        InetSocketAddress isa = new  InetSocketAddress(65535);
        assertEquals("0.0.0.0", isa.getHostName());
        assertEquals(65535, isa.getPort());

        try {
            new  InetSocketAddress(-1);
            fail("IllegalArgumentException was not thrown.");
        } catch(IllegalArgumentException  iae) {
            //expected
        }

        try {
            new  InetSocketAddress(65536);
            fail("IllegalArgumentException was not thrown.");
        } catch(IllegalArgumentException  iae) {
            //expected
        }
    }

    /**
     * java.net.InetSocketAddress#createUnresolved(String, int)
     */
    public void test_createUnresolvedLjava_lang_StringI() {
        HostPortPair[] legalHostPortPairs = { new HostPortPair("127.0.0.1", 1234),
                new HostPortPair("192.168.0.1", 10000), new HostPortPair("127.0.0", 0),
                new HostPortPair("127.0.0", 65535),
                new HostPortPair("strange host", 65535) };
        for (int i = 0; i < legalHostPortPairs.length; i++) {
            InetSocketAddress isa = InetSocketAddress.createUnresolved(
                    legalHostPortPairs[i].host, legalHostPortPairs[i].port);
            assertTrue(isa.isUnresolved());
            assertNull(isa.getAddress());
            assertEquals(isa.getHostName(), legalHostPortPairs[i].host);
            assertEquals(isa.getPort(), legalHostPortPairs[i].port);
        }
    }

    /**
     * java.net.InetSocketAddress#createUnresolved(String, int)
     */
    public void test_createUnresolvedLjava_lang_StringI_IllegalArgumentException() {
        HostPortPair[] illegalHostPortPairs = { new HostPortPair(null, 1),
                new HostPortPair("host", -1), new HostPortPair("host", 65536) };
        for (int i = 0; i < illegalHostPortPairs.length; i++) {
            try {
                InetSocketAddress.createUnresolved(
                        illegalHostPortPairs[i].host,
                        illegalHostPortPairs[i].port);
                fail("should throw IllegalArgumentException, host = "
                        + illegalHostPortPairs[i].host + ",port = "
                        + illegalHostPortPairs[i].port);
            } catch (IllegalArgumentException e) {
                // expected
            }
        }
    }

    /*
     * inner class for createUnresolved test convenience.
     */
    class HostPortPair {
        String host;

        int port;

        public HostPortPair(String host, int port) {
            this.host = host;
            this.port = port;
        }
    };

    // comparator for InetSocketAddress objects
    private static final SerializableAssert COMPARATOR = new SerializableAssert() {
        public void assertDeserialized(Serializable initial,
                Serializable deserialized) {

            InetSocketAddress init = (InetSocketAddress) initial;
            InetSocketAddress desr = (InetSocketAddress) deserialized;

            assertEquals("HostName", init.getHostName(), desr.getHostName());
            assertEquals("Port", init.getPort(), desr.getPort());
            assertEquals("Address", init.getAddress(), desr.getAddress());
        }
    };

    /**
     * serialization/deserialization compatibility.
     */
    public void testSerializationSelf() throws Exception {

        Object[] testCases = {
                InetSocketAddress.createUnresolved("badhost", 1000), // unresolved
                new InetSocketAddress("Localhost", 1000) };

        SerializationTest.verifySelf(testCases, COMPARATOR);
    }

    /**
     * serialization/deserialization compatibility with RI.
     */
    public void testSerializationCompatibility() throws Exception {
        InetAddress localhost = InetAddress.getByAddress("Localhost", new byte[]{127, 0, 0, 1});
        Object[] testCases = {
                InetSocketAddress.createUnresolved("badhost", 1000), // unresolved
                new InetSocketAddress(localhost, 1000) };

        SerializationTest.verifyGolden(this, testCases, COMPARATOR);
    }

    public void test_equals() throws Exception {
        InetSocketAddress isa1 = new InetSocketAddress(1);
        InetSocketAddress isa2 = new InetSocketAddress(2);
        assertFalse(isa1.equals(isa2));
        InetSocketAddress isa3 = new InetSocketAddress(1);
        assertTrue(isa1.equals(isa3));

        InetAddress localhost = InetAddress.getByName("localhost");
        isa1 = new InetSocketAddress(localhost.getHostName(), 80);
        isa2 = new InetSocketAddress(localhost.getHostAddress(), 80);
        assertTrue(isa1.equals(isa2));
    }

    public void test_getAddress() {

        String validIPAddresses[] = { "::1.2.3.4", "::", "::", "1::0", "1::",
                "::1", "0",
                "FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF",
                "FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:255.255.255.255",
                "0:0:0:0:0:0:0:0", "0:0:0:0:0:0:0.0.0.0",
                "127.0.0.1", "localhost", "1.1", "42.42.42.42", "0.0.0.0"};

        for(int i = 0; i < validIPAddresses.length; i++) {
            try {
                InetAddress ia = InetAddress.getByName(validIPAddresses[i]);
                InetSocketAddress isa = new InetSocketAddress(ia, 0);
                assertEquals(ia, isa.getAddress());
            } catch(UnknownHostException uhe) {
                fail("UnknownHostException was thrown for: " +
                        validIPAddresses[i]);
            }
        }
        InetSocketAddress isa = new InetSocketAddress((InetAddress) null, 0);
        assertNotNull(isa.getAddress());
    }

    public void test_hashCode() throws Exception {
        InetAddress localhost = InetAddress.getByName("localhost");
        InetSocketAddress isa1 = new InetSocketAddress(localhost.getHostName(), 8080);
        InetSocketAddress isa2 = new InetSocketAddress(localhost.getHostAddress(), 8080);
        assertTrue(isa1.hashCode() == isa2.hashCode());

        InetSocketAddress isa3 = new InetSocketAddress("0.0.0.0", 8080);
        assertFalse(isa1.hashCode() == isa3.hashCode());
    }

    public void test_isUnresolved() {
        InetSocketAddress isa1 = new InetSocketAddress("localhost", 80);
        assertFalse(isa1.isUnresolved());

        InetSocketAddress sockAddr = new InetSocketAddress("unknown.host", 1000);
        assertTrue(sockAddr.isUnresolved());
    }

    public void test_toString() {
        InetSocketAddress isa = new InetSocketAddress("localhost", 80);
        assertNotNull(isa.toString());
    }

    public void test_getHostString() throws Exception {
        // When we have a hostname, we'll get it back because that doesn't cost a DNS lookup...
        InetSocketAddress hasHostname = InetSocketAddress.createUnresolved("some host", 1234);
        assertEquals("some host", hasHostname.getHostString());
        assertEquals("some host", hasHostname.getHostName());
        // When we don't have a hostname, whether or not we do the reverse lookup is the difference
        // between getHostString and getHostName...
        InetAddress address = InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
        InetSocketAddress noHostname = new InetSocketAddress(address, 1234);
        assertEquals("127.0.0.1", noHostname.getHostString());
        assertEquals("localhost", noHostname.getHostName());
    }
}
