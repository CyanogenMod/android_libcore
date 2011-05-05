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

package org.apache.harmony.luni.tests.java.net;

import dalvik.annotation.BrokenTest;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Enumeration;

import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

import tests.support.Support_Configuration;

public class InetAddressTest extends junit.framework.TestCase {

    private static boolean someoneDone[] = new boolean[2];

    protected static boolean threadedTestSucceeded;

    protected static String threadedTestErrorString;

    private Inet4Address ipv4Localhost;
    private Inet4Address ipv4LoopbackIp;

    @Override protected void setUp() throws Exception {
        super.setUp();
        byte[] ipv4Loopback = { 127, 0, 0, 1 };
        ipv4LoopbackIp = (Inet4Address) InetAddress.getByAddress(ipv4Loopback);
        ipv4Localhost = (Inet4Address) InetAddress.getByAddress("localhost", ipv4Loopback);
    }

    @Override protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * This class is used to test inet_ntoa, gethostbyaddr and gethostbyname
     * functions in the VM to make sure they're threadsafe. getByName will cause
     * the gethostbyname function to be called. getHostName will cause the
     * gethostbyaddr to be called. getHostAddress will cause inet_ntoa to be
     * called.
     */
    static class threadsafeTestThread extends Thread {
        private String lookupName;

        private InetAddress testAddress;

        private int testType;

        /*
         * REP_NUM can be adjusted if desired. Since this error is
         * non-deterministic it may not always occur. Setting REP_NUM higher,
         * increases the chances of an error being detected, but causes the test
         * to take longer. Because the Java threads spend a lot of time
         * performing operations other than running the native code that may not
         * be threadsafe, it is quite likely that several thousand iterations
         * will elapse before the first error is detected.
         */
        private static final int REP_NUM = 20000;

        public threadsafeTestThread(String name, String lookupName,
                InetAddress testAddress, int type) {
            super(name);
            this.lookupName = lookupName;
            this.testAddress = testAddress;
            testType = type;
        }

        public void run() {
            try {
                String correctName = testAddress.getHostName();
                String correctAddress = testAddress.getHostAddress();
                long startTime = System.currentTimeMillis();

                synchronized (someoneDone) {
                }

                for (int i = 0; i < REP_NUM; i++) {
                    if (someoneDone[testType]) {
                        break;
                    } else if ((i % 25) == 0
                            && System.currentTimeMillis() - startTime > 240000) {
                        System.out
                                .println("Exiting due to time limitation after "
                                        + i + " iterations");
                        break;
                    }

                    InetAddress ia = InetAddress.getByName(lookupName);
                    String hostName = ia.getHostName();
                    String hostAddress = ia.getHostAddress();

                    // Intentionally not looking for exact name match so that
                    // the test works across different platforms that may or
                    // may not include a domain suffix on the hostname
                    if (!hostName.startsWith(correctName)) {
                        threadedTestSucceeded = false;
                        threadedTestErrorString = (testType == 0 ? "gethostbyname"
                                : "gethostbyaddr")
                                + ": getHostName() returned "
                                + hostName
                                + " instead of " + correctName;
                        break;
                    }
                    // IP addresses should match exactly
                    if (!correctAddress.equals(hostAddress)) {
                        threadedTestSucceeded = false;
                        threadedTestErrorString = (testType == 0 ? "gethostbyname"
                                : "gethostbyaddr")
                                + ": getHostName() returned "
                                + hostAddress
                                + " instead of " + correctAddress;
                        break;
                    }

                }
                someoneDone[testType] = true;
            } catch (Exception e) {
                threadedTestSucceeded = false;
                threadedTestErrorString = e.toString();
            }
        }
    }

    /**
     * java.net.InetAddress#equals(java.lang.Object)
     */
    public void test_equalsLjava_lang_Object() {
        // Test for method boolean java.net.InetAddress.equals(java.lang.Object)
        assertTrue(ipv4Localhost.equals(ipv4LoopbackIp));
    }

    /**
     * java.net.InetAddress#getAddress()
     */
    public void test_getAddress() {
        // Test for method byte [] java.net.InetAddress.getAddress()
        try {
            InetAddress ia = InetAddress
                    .getByName(Support_Configuration.InetTestIP);
            // BEGIN android-changed
            // using different address. The old one was { 9, 26, -56, -111 }
            // this lead to a crash, also in RI.
            byte[] caddr = Support_Configuration.InetTestAddr;
            // END android-changed
            byte[] addr = ia.getAddress();
            for (int i = 0; i < addr.length; i++)
                assertTrue("Incorrect address returned", caddr[i] == addr[i]);
        } catch (java.net.UnknownHostException e) {
        }
    }

    /**
     * java.net.InetAddress#getAllByName(java.lang.String)
     */
    public void test_getAllByNameLjava_lang_String() throws Exception {
        // Test for method java.net.InetAddress []
        // java.net.InetAddress.getAllByName(java.lang.String)
        InetAddress[] all = InetAddress
                .getAllByName(Support_Configuration.SpecialInetTestAddress);
        assertNotNull(all);
        // Number of aliases depends on individual test machine
        assertTrue(all.length >= 1);
        for (InetAddress alias : all) {
            // Check that each alias has the same hostname. Intentionally not
            // checking for exact string match.
            assertTrue(alias.getHostName().startsWith(
                    Support_Configuration.SpecialInetTestAddress));
        }// end for all aliases

        // Regression for HARMONY-56
        InetAddress[] addresses = InetAddress.getAllByName(null);
        assertTrue("getAllByName(null): no results", addresses.length > 0);
        for (int i = 0; i < addresses.length; i++) {
            InetAddress address = addresses[i];
            assertTrue("Assert 1: getAllByName(null): " + address +
                    " is not loopback", address.isLoopbackAddress());
        }

        try {
            InetAddress.getAllByName("unknown.host");
            fail("UnknownHostException was not thrown.");
        } catch(UnknownHostException uhe) {
            //expected
        }
    }

    /**
     * java.net.InetAddress#getByName(java.lang.String)
     */
    public void test_getByNameLjava_lang_String() throws Exception {
        // Test for method java.net.InetAddress
        // java.net.InetAddress.getByName(java.lang.String)
        InetAddress ia2 = InetAddress
                .getByName(Support_Configuration.InetTestIP);

        // Intentionally not testing for exact string match
        /* FIXME: comment the assertion below because it is platform/configuration dependent
         * Please refer to HARMONY-1664 (https://issues.apache.org/jira/browse/HARMONY-1664)
         * for details
         */
//        assertTrue(
//        "Expected " + Support_Configuration.InetTestAddress + "*",
//            ia2.getHostName().startsWith(Support_Configuration.InetTestAddress));

        // TODO : Test to ensure all the address formats are recognized
        InetAddress i = InetAddress.getByName("1.2.3");
        assertEquals("1.2.0.3",i.getHostAddress());
        i = InetAddress.getByName("1.2");
        assertEquals("1.0.0.2",i.getHostAddress());
        i = InetAddress.getByName(String.valueOf(0xffffffffL));
        assertEquals("255.255.255.255",i.getHostAddress());
        // BEGIN android-removed
        // This test checks a bug in the RI that allows any number of '.' after
        // a valid ipv4 address. This bug doesn't exist in this implementation.
        // String s = "222.222.222.222....";
        // i = InetAddress.getByName(s);
        // assertEquals("222.222.222.222",i.getHostAddress());
        // END android-removed

        class MockSecurityManager extends SecurityManager {
            public void checkPermission(Permission permission) {
                if (permission.getName().equals("setSecurityManager")){
                    return;
                }
                if (permission.getName().equals("3d.com")){
                    throw new SecurityException();
                }
                super.checkPermission(permission);
            }

            public void checkConnect(String host, int port) {
                if(host.equals("google.com")) {
                    throw new SecurityException();
                }
            }
        }

        try {
            InetAddress.getByName("0.0.0.0.0");
            fail("UnknownHostException was not thrown.");
        } catch(UnknownHostException ue) {
            //expected
        }
    }

    /**
     * java.net.InetAddress#getHostAddress()
     */
    public void test_getHostAddress() {
        assertTrue(ipv4Localhost.getHostAddress().equals("127.0.0.1"));
    }

    /**
     * java.net.InetAddress#getHostName()
     */
    public void test_getHostName() throws Exception {
        // Test for method java.lang.String java.net.InetAddress.getHostName()
        InetAddress ia = InetAddress
                .getByName(Support_Configuration.InetTestIP);

        // Intentionally not testing for exact string match
        /* FIXME: comment the assertion below because it is platform/configuration dependent
         * Please refer to HARMONY-1664 (https://issues.apache.org/jira/browse/HARMONY-1664)
         * for details
         */
//        assertTrue(
//        "Expected " + Support_Configuration.InetTestAddress + "*",
//        ia.getHostName().startsWith(Support_Configuration.InetTestAddress));

        // Make sure there is no caching
        System.setProperty("networkaddress.cache.ttl", "0");

        // Test for threadsafety
        assertTrue("127.0.0.1".equals(ipv4LoopbackIp.getHostAddress()));
        assertTrue("127.0.0.1".equals(ipv4Localhost.getHostAddress()));
        threadsafeTestThread thread1 = new threadsafeTestThread("1",
                ipv4LoopbackIp.getHostName(), ipv4LoopbackIp, 0);
        threadsafeTestThread thread2 = new threadsafeTestThread("2",
                ipv4Localhost.getHostName(), ipv4Localhost, 0);
        threadsafeTestThread thread3 = new threadsafeTestThread("3",
                ipv4LoopbackIp.getHostAddress(), ipv4LoopbackIp, 1);
        threadsafeTestThread thread4 = new threadsafeTestThread("4",
                ipv4Localhost.getHostAddress(), ipv4Localhost, 1);

        // initialize the flags
        threadedTestSucceeded = true;
        synchronized (someoneDone) {
            thread1.start();
            thread2.start();
            thread3.start();
            thread4.start();
        }
        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();
        /* FIXME: comment the assertion below because it is platform/configuration dependent
        * Please refer to HARMONY-1664 (https://issues.apache.org/jira/browse/HARMONY-1664)
        * for details
        */
//            assertTrue(threadedTestErrorString, threadedTestSucceeded);
    }

    /**
     * java.net.InetAddress#getLocalHost()
     */
    public void test_getLocalHost() {
        // Test for method java.net.InetAddress
        // java.net.InetAddress.getLocalHost()
        try {
            // We don't know the host name or ip of the machine
            // running the test, so we can't build our own address
            DatagramSocket dg = new DatagramSocket(0, InetAddress.getLocalHost());
            assertTrue("Incorrect host returned", InetAddress.getLocalHost()
                    .equals(dg.getLocalAddress()));
            dg.close();
        } catch (Exception e) {
            fail("Exception during getLocalHost test : " + e.getMessage());
        }
    }

    /**
     * java.net.InetAddress#hashCode()
     */
    int getHashCode(String literal) {
        InetAddress host = null;
        try {
            host = InetAddress.getByName(literal);
        } catch(UnknownHostException e) {
            fail("Exception during hashCode test : " + e.getMessage());
        }
        return host.hashCode();
    }

    public void test_hashCode() {
        int hashCode = getHashCode(Support_Configuration.InetTestIP);
        int ip6HashCode = getHashCode("fe80::20d:60ff:fe24:7410");
        int ip6LOHashCode = getHashCode("::1");
        assertFalse("Hash collision", hashCode == ip6HashCode);
        assertFalse("Hash collision", ip6HashCode == ip6LOHashCode);
        assertFalse("Hash collision", hashCode == ip6LOHashCode);
        assertFalse("Hash collision", ip6LOHashCode == 0);
        assertFalse("Hash collision", ip6LOHashCode == 1);
    }

    /**
     * java.net.InetAddress#isMulticastAddress()
     */
    public void test_isMulticastAddress() {
        // Test for method boolean java.net.InetAddress.isMulticastAddress()
        try {
            InetAddress ia1 = InetAddress.getByName("ff02::1");
            assertTrue("isMulticastAddress returned incorrect result", ia1
                    .isMulticastAddress());
            InetAddress ia2 = InetAddress.getByName("239.255.255.255");
            assertTrue("isMulticastAddress returned incorrect result", ia2
                    .isMulticastAddress());
            InetAddress ia3 = InetAddress.getByName("fefb::");
            assertFalse("isMulticastAddress returned incorrect result", ia3
                    .isMulticastAddress());
            InetAddress ia4 = InetAddress.getByName("10.0.0.1");
            assertFalse("isMulticastAddress returned incorrect result", ia4
                    .isMulticastAddress());
        } catch (Exception e) {
            fail("Exception during isMulticastAddress test : " + e.getMessage());
        }
    }

    /**
     * java.net.InetAddress#toString()
     */
    public void test_toString() throws Exception {
        // Test for method java.lang.String java.net.InetAddress.toString()
        InetAddress ia2 = InetAddress.getByName(Support_Configuration.InetTestIP);
        assertEquals("/" + Support_Configuration.InetTestIP, ia2.toString());
        // Regression for HARMONY-84
        assertEquals("localhost/127.0.0.1", ipv4Localhost.toString());
        assertEquals("/127.0.0.1", ipv4LoopbackIp.toString());
    }

    /**
     * java.net.InetAddress#getByAddress(java.lang.String, byte[])
     */
    public void test_getByAddressLjava_lang_String$B() {
        // Check an IPv4 address with an IPv6 hostname
        byte ipAddress[] = { 127, 0, 0, 1 };
        String addressStr = "::1";
        try {
            InetAddress addr = InetAddress.getByAddress(addressStr, ipAddress);
            addr = InetAddress.getByAddress(ipAddress);
        } catch (UnknownHostException e) {
            fail("Unexpected problem creating IP Address "
                    + ipAddress.length);
        }

        byte ipAddress2[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 127, 0, 0,
                1 };
        addressStr = "::1";
        try {
            InetAddress addr = InetAddress.getByAddress(addressStr, ipAddress2);
            addr = InetAddress.getByAddress(ipAddress);
        } catch (UnknownHostException e) {
            fail("Unexpected problem creating IP Address "
                    + ipAddress.length);
        }

        try {
            InetAddress addr = InetAddress.getByAddress(addressStr,
                    new byte [] {0, 0, 0, 0, 0});
            fail("UnknownHostException was thrown.");
        } catch(UnknownHostException uhe) {
            //expected
        }
    }

    /**
     * java.net.InetAddress#getCanonicalHostName()
     */
    public void test_getCanonicalHostName() throws Exception {
        assertTrue("getCanonicalHostName returned a zero length string ",
                ipv4Localhost.getCanonicalHostName().length() != 0);
        assertTrue("getCanonicalHostName returned an empty string ",
                !ipv4Localhost.equals(""));

        // test against an expected value
        InetAddress ia = InetAddress
                .getByName(Support_Configuration.InetTestIP);

        // Intentionally not testing for exact string match
        /* FIXME: comment the assertion below because it is platform/configuration dependent
         * Please refer to HARMONY-1664 (https://issues.apache.org/jira/browse/HARMONY-1664)
         * for details
         */
//        assertTrue(
//           "Expected " + Support_Configuration.InetTestAddress + "*",
//           ia.getCanonicalHostName().startsWith(Support_Configuration.InetTestAddress));
    }

    /**
     * java.net.InetAddress#isReachableI
     */
    public void test_isReachableI() throws Exception {
        assertTrue(ipv4LoopbackIp.isReachable(10000));
        try {
            ipv4LoopbackIp.isReachable(-1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
    }

    /**
     * java.net.InetAddress#isReachableLjava_net_NetworkInterfaceII
     */
    @BrokenTest("Depends on external network address and shows different" +
            "behavior with WLAN and 3G networks")
    public void test_isReachableLjava_net_NetworkInterfaceII() throws Exception {
        // tests local address
        assertTrue(ipv4LoopbackIp.isReachable(null, 0, 10000));
        InetAddress ia;
        try {
            ipv4LoopbackIp.isReachable(null, -1, 10000);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        try {
            ipv4LoopbackIp.isReachable(null, 0, -1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        try {
            ipv4LoopbackIp.isReachable(null, -1, -1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        // tests nowhere
        ia = Inet4Address.getByName("1.1.1.1");
        assertFalse(ia.isReachable(1000));
        assertFalse(ia.isReachable(null, 0, 1000));

        // Regression test for HARMONY-1842.
        Enumeration<NetworkInterface> nif = NetworkInterface.getNetworkInterfaces();
        NetworkInterface netif;
        while(nif.hasMoreElements()) {
            netif = nif.nextElement();
            ipv4Localhost.isReachable(netif, 10, 1000);
        }
    }

    // comparator for InetAddress objects
    private static final SerializableAssert COMPARATOR = new SerializableAssert() {
        public void assertDeserialized(Serializable initial,
                Serializable deserialized) {

            InetAddress initAddr = (InetAddress) initial;
            InetAddress desrAddr = (InetAddress) deserialized;

            byte[] iaAddresss = initAddr.getAddress();
            byte[] deIAAddresss = desrAddr.getAddress();
            for (int i = 0; i < iaAddresss.length; i++) {
                assertEquals(iaAddresss[i], deIAAddresss[i]);
            }
            assertEquals(initAddr.getHostName(), desrAddr.getHostName());
        }
    };

    // Regression Test for Harmony-2290
    public void test_isReachableLjava_net_NetworkInterfaceII_loopbackInterface() throws IOException {
        final int TTL = 20;
        final int TIME_OUT = 3000;

        NetworkInterface loopbackInterface = null;
        ArrayList<InetAddress> localAddresses = new ArrayList<InetAddress>();
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
                .getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> addresses = networkInterface
                    .getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address.isLoopbackAddress()) {
                    loopbackInterface = networkInterface;
                } else {
                    localAddresses.add(address);
                }
            }
        }

        //loopbackInterface can reach local address
        if (null != loopbackInterface) {
            for (InetAddress destAddress : localAddresses) {
                assertTrue(destAddress.isReachable(loopbackInterface, TTL, TIME_OUT));
            }
        }

        //loopback Interface cannot reach outside address
        InetAddress destAddress = InetAddress.getByName("www.google.com");
        assertFalse(destAddress.isReachable(loopbackInterface, TTL, TIME_OUT));
    }

    /**
     * serialization/deserialization compatibility.
     */
    public void testSerializationSelf() throws Exception {
        SerializationTest.verifySelf(ipv4Localhost, COMPARATOR);
    }

    /**
     * serialization/deserialization compatibility with RI.
     */
    public void testSerializationCompatibility() throws Exception {
        SerializationTest.verifyGolden(this, ipv4Localhost, COMPARATOR);
    }

    /**
     * java.net.InetAddress#getByAddress(byte[])
     */
    public void test_getByAddress() {
        byte ipAddress[] = { 127, 0, 0, 1 };
        try {
            InetAddress.getByAddress(ipAddress);
        } catch (UnknownHostException e) {
            fail("Unexpected problem creating IP Address "
                    + ipAddress.length);
        }

        byte ipAddress2[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 127, 0, 0,
                1 };
        try {
            InetAddress.getByAddress(ipAddress2);
        } catch (UnknownHostException e) {
            fail("Unexpected problem creating IP Address "
                    + ipAddress.length);
        }

        // Regression for HARMONY-61
        try {
            InetAddress.getByAddress(null);
            fail("Assert 0: UnknownHostException must be thrown");
        } catch (UnknownHostException e) {
            // Expected
        }

        try {
            byte [] byteArray = new byte[] {};
            InetAddress.getByAddress(byteArray);
            fail("Assert 1: UnknownHostException must be thrown");
        } catch (UnknownHostException e) {
            // Expected
        }
    }

    public void test_isAnyLocalAddress() throws Exception {
        byte [] ipAddress1 = { 127, 42, 42, 42 };
        InetAddress ia1 = InetAddress.getByAddress(ipAddress1);
        assertFalse(ia1.isAnyLocalAddress());

        byte [] ipAddress2 = { 0, 0, 0, 0 };
        InetAddress ia2 = InetAddress.getByAddress(ipAddress2);
        assertTrue(ia2.isAnyLocalAddress());
    }

    public void test_isLinkLocalAddress() throws Exception {
        String addrName = "FE80::0";
        InetAddress addr = InetAddress.getByName(addrName);
        assertTrue(
                "IPv6 link local address " + addrName + " not detected.",
                addr.isLinkLocalAddress());

        addrName = "FEBF::FFFF:FFFF:FFFF:FFFF";
        addr = Inet6Address.getByName(addrName);
        assertTrue(
                "IPv6 link local address " + addrName + " not detected.",
                addr.isLinkLocalAddress());

        addrName = "FEC0::1";
        addr = Inet6Address.getByName(addrName);
        assertTrue("IPv6 address " + addrName
                + " detected incorrectly as a link local address.", !addr
                .isLinkLocalAddress());

        addrName = "42.42.42.42";
        addr = Inet4Address.getByName(addrName);
        assertTrue("IPv4 address " + addrName
                + " incorrectly reporting as a link local address.", !addr
                .isLinkLocalAddress());
    }

    public void test_isLoopbackAddress() throws Exception {
        String addrName = "127.0.0.0";
        assertTrue("Loopback address " + addrName + " not detected.",
                ipv4LoopbackIp.isLoopbackAddress());

        addrName = "127.42.42.42";
        InetAddress addr = InetAddress.getByName(addrName);
        assertTrue("Loopback address " + addrName + " not detected.", addr
                .isLoopbackAddress());

        addrName = "42.42.42.42";
        addr = Inet4Address.getByName(addrName);
        assertTrue("Address incorrectly " + addrName
                + " detected as a loopback address.", !addr
                .isLoopbackAddress());


        addrName = "::FFFF:127.42.42.42";
        addr = InetAddress.getByName(addrName);
        assertTrue("IPv4-compatible IPv6 loopback address " + addrName
                + " not detected.", addr.isLoopbackAddress());

        addrName = "::FFFF:42.42.42.42";
        addr = InetAddress.getByName(addrName);
        assertTrue("IPv4-compatible IPv6 address incorrectly " + addrName
                + " detected as a loopback address.", !addr
                .isLoopbackAddress());
    }

    public void test_isMCGlobal() throws Exception {
        String addrName = "224.0.0.255";
        InetAddress addr = InetAddress.getByName(addrName);
        assertTrue("IPv4 link-local multicast address " + addrName
                + " incorrectly identified as a global multicast address.",
                !addr.isMCGlobal());

        addrName = "224.0.1.0"; // a multicast addr 1110
        addr = Inet4Address.getByName(addrName);
        assertTrue("IPv4 global multicast address " + addrName
                + " not identified as a global multicast address.", addr
                .isMCGlobal());

        addrName = "FFFE:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF";
        addr = InetAddress.getByName(addrName);
        assertTrue("IPv6 global multicast address " + addrName
                + " not detected.", addr.isMCGlobal());

        addrName = "FF08:42:42:42:42:42:42:42";
        addr = InetAddress.getByName(addrName);
        assertTrue("IPv6 mulitcast organizational " + addrName
                + " incorrectly indicated as a global address.", !addr
                .isMCGlobal());
    }

    public void test_isMCLinkLocal() throws Exception {
        String addrName = "224.0.0.255";
        InetAddress addr = InetAddress.getByName(addrName);
        assertTrue("IPv4 link-local multicast address " + addrName
                + " not identified as a link-local multicast address.",
                addr.isMCLinkLocal());

        addrName = "224.0.1.0";
        addr = InetAddress.getByName(addrName);
        assertTrue(
                "IPv4 global multicast address "
                        + addrName
                        + " incorrectly identified as a link-local " +
                                "multicast address.",
                !addr.isMCLinkLocal());

        addrName = "FFF2:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF";
        addr = InetAddress.getByName(addrName);
        assertTrue("IPv6 link local multicast address " + addrName
                + " not detected.", addr.isMCLinkLocal());

        addrName = "FF08:42:42:42:42:42:42:42";
        addr = InetAddress.getByName(addrName);
        assertTrue(
                "IPv6 organization multicast address "
                        + addrName
                        + " incorrectly indicated as a link-local " +
                                "mulitcast address.",
                !addr.isMCLinkLocal());
    }

    public void test_isMCNodeLocal() throws Exception {
        String addrName = "224.42.42.42";
        InetAddress addr = InetAddress.getByName(addrName);
        assertTrue(
                "IPv4 multicast address "
                        + addrName
                        + " incorrectly identified as a node-local " +
                                "multicast address.",
                !addr.isMCNodeLocal());

        addrName = "FFF1:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF";
        addr = InetAddress.getByName(addrName);
        assertTrue("IPv6 node-local multicast address " + addrName
                + " not detected.", addr.isMCNodeLocal());

        addrName = "FF08:42:42:42:42:42:42:42";
        addr = InetAddress.getByName(addrName);
        assertTrue("IPv6 mulitcast organizational address " + addrName
                + " incorrectly indicated as a node-local address.", !addr
                .isMCNodeLocal());
    }

    public void test_isMCOrgLocal() throws Exception {
        String addrName = "239.252.0.0"; // a multicast addr 1110
        InetAddress addr = InetAddress.getByName(addrName);
        assertTrue(
                "IPv4 site-local multicast address "
                        + addrName
                        + " incorrectly identified as a org-local multicast address.",
                !addr.isMCOrgLocal());

        addrName = "239.192.0.0"; // a multicast addr 1110
        addr = InetAddress.getByName(addrName);
        assertTrue("IPv4 org-local multicast address " + addrName
                + " not identified as a org-local multicast address.", addr
                .isMCOrgLocal());

        addrName = "FFF8:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF";
        addr = InetAddress.getByName(addrName);
        assertTrue("IPv6 organization-local multicast address " + addrName
                + " not detected.", addr.isMCOrgLocal());

        addrName = "FF0E:42:42:42:42:42:42:42";
        addr = InetAddress.getByName(addrName);
        assertTrue(
                "IPv6 global multicast address "
                        + addrName
                        + " incorrectly indicated as an organization-local mulitcast address.",
                !addr.isMCOrgLocal());
    }

    public void test_isMCSiteLocal() throws Exception {
        String addrName = "FFF5:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF";
        InetAddress addr = InetAddress.getByName(addrName);
        assertTrue("IPv6 site-local multicast address " + addrName
                + " not detected.", addr.isMCSiteLocal());

        // a sample MC organizational address
        addrName = "FF08:42:42:42:42:42:42:42";
        addr = Inet6Address.getByName(addrName);
        assertTrue(
                "IPv6 organization multicast address "
                        + addrName
                        + " incorrectly indicated as a site-local " +
                                "mulitcast address.",
                !addr.isMCSiteLocal());

        addrName = "239.0.0.0";
        addr = Inet4Address.getByName(addrName);
        assertTrue(
                "IPv4 reserved multicast address "
                        + addrName
                        + " incorrectly identified as a site-local " +
                                "multicast address.",
                !addr.isMCSiteLocal());

        addrName = "239.255.0.0";
        addr = Inet4Address.getByName(addrName);
        assertTrue("IPv4 site-local multicast address " + addrName
                + " not identified as a site-local multicast address.",
                addr.isMCSiteLocal());
    }

    public void test_isSiteLocalAddress() throws Exception {
        String addrName = "42.42.42.42";
        InetAddress addr = InetAddress.getByName(addrName);
        assertTrue("IPv4 address " + addrName
                + " incorrectly reporting as a site local address.", !addr
                .isSiteLocalAddress());

        addrName = "FEFF::FFFF:FFFF:FFFF:FFFF:FFFF";
        addr = InetAddress.getByName(addrName);
        assertTrue(
                "IPv6 site local address " + addrName + " not detected.",
                addr.isSiteLocalAddress());

        addrName = "FEBF::FFFF:FFFF:FFFF:FFFF:FFFF";
        addr = InetAddress.getByName(addrName);
        assertTrue("IPv6 address " + addrName
                + " detected incorrectly as a site local address.", !addr
                .isSiteLocalAddress());
    }

    class MockSecurityManager extends SecurityManager {
        public void checkPermission(Permission permission) {
            if (permission.getName().equals("setSecurityManager")){
                return;
            }
            if (permission.getName().equals("3d.com")){
                throw new SecurityException();
            }
            super.checkPermission(permission);
        }
    }
}
