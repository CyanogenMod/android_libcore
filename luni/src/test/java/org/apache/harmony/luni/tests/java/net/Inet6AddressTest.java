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

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;

import java.security.Permission;

import org.apache.harmony.luni.tests.java.net.InetAddressTest.MockSecurityManager;
import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

public class Inet6AddressTest extends junit.framework.TestCase {

    private Inet6Address ipv6Localhost;
    private Inet6Address ipv6LoopbackIp;

    @Override protected void setUp() throws Exception {
        super.setUp();
        byte[] ipv6Loopback = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };
        ipv6LoopbackIp = (Inet6Address) InetAddress.getByAddress(ipv6Loopback);
        ipv6Localhost = (Inet6Address) InetAddress.getByAddress("localhost", ipv6Loopback);
    }
    /**
     * java.net.Inet6Address#isMulticastAddress()
     */
    public void test_isMulticastAddress() {

        String addrName = "";
        InetAddress addr = null;

        try {

            // IP V6 regular multicast and non-multicast tests
            //
            // Create 2 IP v6 addresses and call "isMulticastAddress()"
            // A prefix of "11111111" means that the address is multicast
            // The first one will be one with the prefix the second without

            addrName = "FFFF::42:42"; // 11111111 = FFFF
            addr = InetAddress.getByName(addrName);
            assertTrue("Multicast address " + addrName + " not detected.", addr
                    .isMulticastAddress());

            addrName = "42::42:42"; // an non-multicast address
            addr = InetAddress.getByName(addrName);
            assertTrue("Non multicast address " + addrName
                    + " reporting as a multicast address.", !addr
                    .isMulticastAddress());

            // IPv4-compatible IPv6 address tests
            //
            // Now create 2 IP v6 addresses that are IP v4 compatable
            // to IP v6 addresses. The address prefix for a multicast ip v4
            // address is 1110 for the last 16 bits ::d.d.d.d
            // We expect these to be false

            addrName = "::224.42.42.42"; // an ipv4 multicast addr 1110 = 224
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4 compatable address " + addrName
                    + " reported incorrectly as multicast.", !addr
                    .isMulticastAddress());

            addrName = "::42.42.42.42"; // an ipv4 non-multicast address
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4 compatable address " + addrName
                    + " reported incorrectly as multicast.", !addr
                    .isMulticastAddress());

            // IPv4-mapped IPv6 address tests
            //
            // Now create 2 IP v6 addresses that are IP v4 compatable
            // to IP v6 addresses. The address prefix for a multicast ip v4
            // address is 1110 for the last 16 bits ::FFFF:d.d.d.d

            addrName = "::FFFF:224.42.42.42"; // an ipv4 multicast addr 1110 =
            // 224
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4-mapped IPv6 multicast address " + addrName
                    + " not detected.", addr.isMulticastAddress());

            addrName = "::FFFF:42.42.42.42"; // an ipv4 non-multicast address
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4-mapped IPv6 non-multicast address " + addrName
                    + " reporting as a multicast address.", !addr
                    .isMulticastAddress());
        } catch (Exception e) {
            fail("Unknown address : " + addrName);
        }
    }

    /**
     * java.net.Inet6Address#isAnyLocalAddress()
     */
    public void test_isAnyLocalAddress() {

        String addrName = "";
        InetAddress addr = null;

        try {

            // test to ensure that the unspecified address returns tru
            addrName = "::0"; // The unspecified address
            addr = InetAddress.getByName(addrName);
            assertTrue(
                    "The unspecified (also known as wildcard and any local address) "
                            + addrName + " not detected.", addr
                            .isAnyLocalAddress());

            addrName = "::"; // another form of the unspecified address
            addr = InetAddress.getByName(addrName);
            assertTrue(
                    "The unspecified (also known as wildcard and any local address) "
                            + addrName + " not detected.", addr
                            .isAnyLocalAddress());

            addrName = "::1"; // The loopback address
            addr = InetAddress.getByName(addrName);
            assertTrue("The addresses " + addrName
                    + " incorrectly reporting an the unspecified address.",
                    !addr.isAnyLocalAddress());

        } catch (Exception e) {
            fail("Unknown address : " + addrName);
        }
    }

    /**
     * java.net.Inet6Address#isLoopbackAddress()
     */
    public void test_isLoopbackAddress() {

        String addrName = "";
        try {

            // IP V6 regular address tests for loopback
            // The loopback address for IPv6 is ::1

            addrName = "::1";
            InetAddress addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 loopback address " + addrName + " not detected.",
                    addr.isLoopbackAddress());

            addrName = "::2";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 address incorrectly " + addrName
                    + " detected as a loopback address.", !addr
                    .isLoopbackAddress());

            // a loopback address should be 127.d.d.d
            addrName = "42:42::42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 address incorrectly " + addrName
                    + " detected as a loopback address.", !addr
                    .isLoopbackAddress());

            // IPv4-compatible IPv6 address tests
            //
            // Now create 2 IP v6 addresses that are IP v4 compatable
            // to IP v6 addresses. The address prefix for a multicast ip v4
            // address is 1110 for the last 16 bits ::d.d.d.d
            // We expect these to be false, as they are not IPv4 addresses

            // a loopback address should be 127.d.d.d
            addrName = "::127.0.0.0";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4-compatible IPv6 address " + addrName
                    + " detected incorrectly as a loopback.", !addr
                    .isLoopbackAddress());

            addrName = "::127.42.42.42"; // a loopback address should be
            // 127.d.d.d
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4-compatible IPv6 address " + addrName
                    + " detected incorrectly as a loopback.", !addr
                    .isLoopbackAddress());

            // a loopback address should be 127.d.d.d
            addrName = "::42.42.42.42";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4-compatible IPv6 address " + addrName
                    + " detected incorrectly as a loopback.", !addr
                    .isLoopbackAddress());

            // IPv4-mapped IPv6 address tests
            //
            // Now create 2 IP v6 addresses that are IP v4 compatable
            // to IP v6 addresses. The address prefix for a multicast ip v4
            // address is 1110 for the last 16 bits ::FFFF:d.d.d.d

            // a loopback address should be 127.d.d.d
            addrName = "::FFFF:127.0.0.0";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4-compatible IPv6 loopback address " + addrName
                    + " not detected.", addr.isLoopbackAddress());

            // a loopback address should be 127.d.d.d
            addrName = "::FFFF:127.42.42.42";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4-compatible IPv6 loopback address " + addrName
                    + " not detected.", addr.isLoopbackAddress());

            // a loopback address should be 127.d.d.d
            addrName = "::FFFF:42.42.42.42";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4-compatible IPv6 address incorrectly " + addrName
                    + " detected as a loopback address.", !addr
                    .isLoopbackAddress());

        } catch (UnknownHostException e) {
            fail("Unknown address : " + addrName);
        }
    }

    /**
     * java.net.Inet6Address#isLinkLocalAddress()
     */
    public void test_isLinkLocalAddress() {

        String addrName = "";
        try {
            // IP V6 regular address tests for link local addresses
            //
            // Link local addresses are FE80:: -
            // FEBF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF

            addrName = "FE80::0";
            InetAddress addr = InetAddress.getByName(addrName);
            assertTrue(
                    "IPv6 link local address " + addrName + " not detected.",
                    addr.isLinkLocalAddress());

            addrName = "FEBF::FFFF:FFFF:FFFF:FFFF";
            addr = InetAddress.getByName(addrName);
            assertTrue(
                    "IPv6 link local address " + addrName + " not detected.",
                    addr.isLinkLocalAddress());

            addrName = "FEC0::1";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 address " + addrName
                    + " detected incorrectly as a link local address.", !addr
                    .isLinkLocalAddress());

            addrName = "FD80::1:FFFF:FFFF:FFFF:FFFF";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 address " + addrName
                    + " detected incorrectly as a link local address.", !addr
                    .isLinkLocalAddress());

            addrName = "FE7F::FFFF:FFFF:FFFF:FFFF";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 address " + addrName
                    + " detected incorrectly as a link local address.", !addr
                    .isLinkLocalAddress());
        } catch (Exception e) {
            fail("Unknown address : " + addrName);
        }

    }

    /**
     * java.net.Inet6Address#isSiteLocalAddress()
     */
    public void test_isSiteLocalAddress() {
        String addrName = "";
        try {
            // IP V6 regular address tests for link local addresses
            //
            // Link local addresses are FEC0::0 through to
            // FEFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF

            addrName = "FEC0::0";
            InetAddress addr = InetAddress.getByName(addrName);
            assertTrue(
                    "IPv6 site local address " + addrName + " not detected.",
                    addr.isSiteLocalAddress());

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

            addrName = "FFC0::0";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 address " + addrName
                    + " detected incorrectly as a site local address.", !addr
                    .isSiteLocalAddress());

        } catch (Exception e) {
            fail("Unknown address : " + addrName);
        }
    }

    /**
     * java.net.Inet6Address#isMCGlobal()
     */
    public void test_isMCGlobal() {
        String addrName = "";
        try {
            // IP V6 regular address tests for Mulitcase Global addresses
            //
            // Multicast global addresses are FFxE:/112 where x is
            // a set of flags, and the addition 112 bits make up
            // the global address space

            addrName = "FF0E::0";
            InetAddress addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 global mutlicast address " + addrName
                    + " not detected.", addr.isMCGlobal());

            addrName = "FF0E:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 global multicast address " + addrName
                    + " not detected.", addr.isMCGlobal());

            // a currently invalid address as the prefix FFxE
            // is only valid for x = {1,0} as the rest are reserved
            addrName = "FFFE::0";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 global mutlicast address " + addrName
                    + " not detected.", addr.isMCGlobal());

            // a currently invalid address as the prefix FFxE
            // is only valid for x = {1,0} as the rest are reserved
            addrName = "FFFE:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 global multicast address " + addrName
                    + " not detected.", addr.isMCGlobal());

            // a sample MC organizational address
            addrName = "FF08:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 mulitcast organizational " + addrName
                    + " incorrectly indicated as a global address.", !addr
                    .isMCGlobal());

            // a sample MC site address
            addrName = "FF05:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 mulitcast site address " + addrName
                    + " incorrectly indicated as a global address.", !addr
                    .isMCGlobal());

            // a sample MC link address
            addrName = "FF02:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 mulitcast link address " + addrName
                    + " incorrectly indicated as a global address.", !addr
                    .isMCGlobal());

            // a sample MC Node
            addrName = "FF01:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 mulitcast node address " + addrName
                    + " incorrectly indicated as a global address.", !addr
                    .isMCGlobal());

            // IPv4-mapped IPv6 address tests
            addrName = "::FFFF:224.0.1.0";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4 global multicast address " + addrName
                    + " not identified as a global multicast address.", addr
                    .isMCGlobal());

            addrName = "::FFFF:238.255.255.255";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4 global multicast address " + addrName
                    + " not identified as a global multicast address.", addr
                    .isMCGlobal());

        } catch (Exception e) {
            fail("Unknown address : " + addrName);
        }
    }

    /**
     * java.net.Inet6Address#isMCNodeLocal()
     */
    public void test_isMCNodeLocal() {
        String addrName = "";
        try {
            // IP V6 regular address tests for Mulitcase node local addresses
            //
            // Multicast node local addresses are FFx1:/112 where x is
            // a set of flags, and the addition 112 bits make up
            // the global address space

            addrName = "FF01::0";
            InetAddress addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 node-local mutlicast address " + addrName
                    + " not detected.", addr.isMCNodeLocal());

            addrName = "FF01:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 node-local multicast address " + addrName
                    + " not detected.", addr.isMCNodeLocal());

            // a currently invalid address as the prefix FFxE
            // is only valid for x = {1,0} as the rest are reserved
            addrName = "FFF1::0";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 node-local mutlicast address " + addrName
                    + " not detected.", addr.isMCNodeLocal());

            // a currently invalid address as the prefix FFxE
            // is only valid for x = {1,0} as the rest are reserved
            addrName = "FFF1:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 node-local multicast address " + addrName
                    + " not detected.", addr.isMCNodeLocal());

            // a sample MC organizational address
            addrName = "FF08:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 mulitcast organizational address " + addrName
                    + " incorrectly indicated as a node-local address.", !addr
                    .isMCNodeLocal());

            // a sample MC site address
            addrName = "FF05:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 mulitcast site address " + addrName
                    + " incorrectly indicated as a node-local address.", !addr
                    .isMCNodeLocal());

            // a sample MC link address
            addrName = "FF02:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 mulitcast link address " + addrName
                    + " incorrectly indicated as a node-local address.", !addr
                    .isMCNodeLocal());

            // a sample MC global address
            addrName = "FF0E:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 mulitcast node address " + addrName
                    + " incorrectly indicated as a node-local address.", !addr
                    .isMCNodeLocal());

        } catch (Exception e) {
            fail("Unknown address : " + addrName);
        }
    }

    /**
     * java.net.Inet6Address#isMCLinkLocal()
     */
    public void test_isMCLinkLocal() {
        String addrName = "";
        try {
            // IP V6 regular address tests for Mulitcase link local addresses
            //
            // Multicast link local addresses are FFx2:/112 where x is
            // a set of flags, and the addition 112 bits make up
            // the global address space

            addrName = "FF02::0";
            InetAddress addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 link local multicast address " + addrName
                    + " not detected.", addr.isMCLinkLocal());

            addrName = "FF02:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 link local multicast address " + addrName
                    + " not detected.", addr.isMCLinkLocal());

            // a currently invalid address as the prefix FFxE
            // is only valid for x = {1,0} as the rest are reserved
            addrName = "FFF2::0";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 link local multicast address " + addrName
                    + " not detected.", addr.isMCLinkLocal());

            // a currently invalid address as the prefix FFxE
            // is only valid for x = {1,0} as the rest are reserved
            addrName = "FFF2:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 link local multicast address " + addrName
                    + " not detected.", addr.isMCLinkLocal());

            // a sample MC organizational address
            addrName = "FF08:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue(
                    "IPv6 organization multicast address "
                            + addrName
                            + " incorrectly indicated as a link-local mulitcast address.",
                    !addr.isMCLinkLocal());

            // a sample MC site address
            addrName = "FF05:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue(
                    "IPv6 site-local mulitcast address "
                            + addrName
                            + " incorrectly indicated as a link-local mulitcast address.",
                    !addr.isMCLinkLocal());

            // a sample MC global address
            addrName = "FF0E:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue(
                    "IPv6 global multicast address "
                            + addrName
                            + " incorrectly indicated as a link-local mulitcast address.",
                    !addr.isMCLinkLocal());

            // a sample MC Node
            addrName = "FF01:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue(
                    "IPv6 mulitcast node address "
                            + addrName
                            + " incorrectly indicated as a link-local mulitcast address.",
                    !addr.isMCLinkLocal());

            // Ipv4-mapped IPv6 addresses

            addrName = "::FFFF:224.0.0.0"; // a multicast addr 1110
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4 link-local multicast address " + addrName
                    + " not identified as a link-local multicast address.",
                    addr.isMCLinkLocal());

            addrName = "::FFFF:224.0.0.255"; // a multicast addr 1110
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4 link-local multicast address " + addrName
                    + " not identified as a link-local multicast address.",
                    addr.isMCLinkLocal());

        } catch (Exception e) {
            fail("Unknown address : " + addrName);
        }
    }

    /**
     * java.net.Inet6Address#isMCSiteLocal()
     */
    public void test_isMCSiteLocal() {
        String addrName = "";
        try {
            // IP V6 regular address tests for Multicast site-local addresses
            //
            // Multicast global addresses are FFx5:/112 where x is
            // a set of flags, and the addition 112 bits make up
            // the global address space

            addrName = "FF05::0";
            InetAddress addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 site-local mutlicast address " + addrName
                    + " not detected.", addr.isMCSiteLocal());

            addrName = "FF05:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 site-local multicast address " + addrName
                    + " not detected.", addr.isMCSiteLocal());

            // a currently invalid address as the prefix FFxE
            // is only valid for x = {1,0} as the rest are reserved
            addrName = "FFF5::0";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 site-local mutlicast address " + addrName
                    + " not detected.", addr.isMCSiteLocal());

            // a currently invalid address as the prefix FFxE
            // is only valid for x = {1,0} as the rest are reserved
            addrName = "FFF5:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 site-local multicast address " + addrName
                    + " not detected.", addr.isMCSiteLocal());

            // a sample MC organizational address
            addrName = "FF08:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue(
                    "IPv6 organization multicast address "
                            + addrName
                            + " incorrectly indicated as a site-local mulitcast address.",
                    !addr.isMCSiteLocal());

            // a sample MC global address
            addrName = "FF0E:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue(
                    "IPv6 global mulitcast address "
                            + addrName
                            + " incorrectly indicated as a site-local mulitcast address.",
                    !addr.isMCSiteLocal());

            // a sample MC link address
            addrName = "FF02:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue(
                    "IPv6 link-local multicast address "
                            + addrName
                            + " incorrectly indicated as a site-local mulitcast address.",
                    !addr.isMCSiteLocal());

            // a sample MC Node
            addrName = "FF01:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue(
                    "IPv6 mulitcast node address "
                            + addrName
                            + " incorrectly indicated as a site-local mulitcast address.",
                    !addr.isMCSiteLocal());

            // IPv4-mapped IPv6 addresses
            addrName = "::FFFF:239.255.0.0";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4 site-local multicast address " + addrName
                    + " not identified as a site-local multicast address.",
                    addr.isMCSiteLocal());

            addrName = "::FFFF:239.255.255.255";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4 site-local multicast address " + addrName
                    + " not identified as a site-local multicast address.",
                    addr.isMCSiteLocal());

        } catch (Exception e) {
            fail("Unknown address : " + addrName);
        }
    }

    /**
     * java.net.Inet6Address#isMCOrgLocal()
     */
    public void test_isMCOrgLocal() {
        String addrName = "";
        try {
            // IP V6 regular address tests for Mulitcase organization-local
            // addresses
            //
            // Multicast global addresses are FFxE:/112 where x is
            // a set of flags, and the addition 112 bits make up
            // the global address space

            addrName = "FF08::0";
            InetAddress addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 organization-local mutlicast address " + addrName
                    + " not detected.", addr.isMCOrgLocal());

            addrName = "FF08:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 organization-local multicast address " + addrName
                    + " not detected.", addr.isMCOrgLocal());

            // a currently invalid address as the prefix FFxE
            // is only valid for x = {1,0} as the rest are reserved
            addrName = "FFF8::0";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 organization-local mutlicast address " + addrName
                    + " not detected.", addr.isMCOrgLocal());

            // a currently invalid address as the prefix FFxE
            // is only valid for x = {1,0} as the rest are reserved
            addrName = "FFF8:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv6 organization-local multicast address " + addrName
                    + " not detected.", addr.isMCOrgLocal());

            // a sample MC global address
            addrName = "FF0E:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue(
                    "IPv6 global multicast address "
                            + addrName
                            + " incorrectly indicated as an organization-local mulitcast address.",
                    !addr.isMCOrgLocal());

            // a sample MC site address
            addrName = "FF05:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue(
                    "IPv6 site-local mulitcast address "
                            + addrName
                            + " incorrectly indicated as an organization-local mulitcast address.",
                    !addr.isMCOrgLocal());

            // a sample MC link address
            addrName = "FF02:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue(
                    "IPv6 link-local multicast address "
                            + addrName
                            + " incorrectly indicated as an organization-local mulitcast address.",
                    !addr.isMCOrgLocal());

            // a sample MC Node
            addrName = "FF01:42:42:42:42:42:42:42";
            addr = InetAddress.getByName(addrName);
            assertTrue(
                    "IPv6 mulitcast node address "
                            + addrName
                            + " incorrectly indicated as an organization-local mulitcast address.",
                    !addr.isMCOrgLocal());

            // IPv4-mapped IPv6 addresses

            addrName = "::FFFF:239.192.0.0";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4 org-local multicast address " + addrName
                    + " not identified as a org-local multicast address.", addr
                    .isMCOrgLocal());

            addrName = "::FFFF:239.195.255.255";
            addr = InetAddress.getByName(addrName);
            assertTrue("IPv4 org-local multicast address " + addrName
                    + " not identified as a org-local multicast address.", addr
                    .isMCOrgLocal());

        } catch (Exception e) {
            fail("Unknown address : " + addrName);
        }
    }

    /**
     * java.net.Inet6Address#isIPv4CompatibleAddress()
     */
    public void test_isIPv4CompatibleAddress() {
        String addrName = "";
        Inet6Address addr = null;

        try {

            // Tests a number of addresses to see if they are compatable with
            // IPv6 addresses

            addrName = "FFFF::42:42"; // 11111111 = FFFF
            addr = (Inet6Address) InetAddress.getByName(addrName);
            assertTrue("A non-compatable IPv6 address " + addrName
                    + " incorrectly identified as a IPv4 compatable address.",
                    !addr.isIPv4CompatibleAddress());

            // IPv4-compatible IPv6 address tests
            //
            // Now create 2 IP v6 addresses that are IP v4 compatable
            // to IP v6 addresses.

            addrName = "::0.0.0.0";
            addr = (Inet6Address) InetAddress.getByName(addrName);
            assertTrue("IPv4 compatable address " + addrName
                    + " not detected correctly.", addr
                    .isIPv4CompatibleAddress());

            addrName = "::255.255.255.255"; // an ipv4 non-multicast address
            addr = (Inet6Address) InetAddress.getByName(addrName);
            assertTrue("IPv4 compatable address " + addrName
                    + " not detected correctly.", addr
                    .isIPv4CompatibleAddress());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unknown address : " + addrName);
        }
    }

    /**
     * java.net.Inet6Address#getByName(java.lang.String)
     */
    public void test_getByNameLjava_lang_String() throws Exception {
        // ones to add "::255.255.255.255", "::FFFF:0.0.0.0",
        // "0.0.0.0.0.0::255.255.255.255", "F:F:F:F:F:F:F:F",
        // "[F:F:F:F:F:F:F:F]"
        String validIPAddresses[] = { "::1.2.3.4", "::", "::", "1::0", "1::",
                "::1", "0", /* jdk1.5 accepts 0 as valid */
                "FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF",
                "FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:255.255.255.255",
                "0:0:0:0:0:0:0:0", "0:0:0:0:0:0:0.0.0.0" };

        String invalidIPAddresses[] = { "FFFF:FFFF" };

        for (int i = 0; i < validIPAddresses.length; i++) {

            InetAddress.getByName(validIPAddresses[i]);

            //exercise positive cache
            InetAddress.getByName(validIPAddresses[i]);

            if (!validIPAddresses[i].equals("0")) {
                String tempIPAddress = "[" + validIPAddresses[i] + "]";
                InetAddress.getByName(tempIPAddress);
            }
        }

        for (int i = 0; i < invalidIPAddresses.length; i++) {
            try {
                InetAddress.getByName(invalidIPAddresses[i]);
                fail("Invalid IP address incorrectly recognized as valid: "
                        + invalidIPAddresses[i]);
            } catch (Exception e) {
            }

            //exercise negative cache
            try {
                InetAddress.getByName(invalidIPAddresses[i]);
                fail("Invalid IP address incorrectly recognized as valid: "
                        + invalidIPAddresses[i]);
            } catch (Exception e) {
            }
        }
    }

    /**
     * java.net.Inet6Address#getByAddress(String, byte[], int)
     */
    public void test_getByAddressLString$BI() throws UnknownHostException{
        try {
            Inet6Address.getByAddress("123", null, 0);
            fail("should throw UnknownHostException");
        } catch (UnknownHostException uhe) {
            // expected
        }
        byte[] addr1 = { (byte) 127, 0, 0, 1 };
        try {
            Inet6Address.getByAddress("123", addr1, 0);
            fail("should throw UnknownHostException");
        } catch (UnknownHostException uhe) {
            // expected
        }

        byte[] addr2 = { (byte) 0xFE, (byte) 0x80, 0, 0, 0, 0, 0, 0, 0x02,
                0x11, 0x25, (byte) 0xFF, (byte) 0xFE, (byte) 0xF8, (byte) 0x7C,
                (byte) 0xB2 };

        // should not throw any exception
        Inet6Address.getByAddress("123", addr2, 3);
        Inet6Address.getByAddress("123", addr2, 0);
        Inet6Address.getByAddress("123", addr2, -1);
    }

    /**
     * java.net.Inet6Address#getByAddress(String, byte[],
     *        NetworkInterface)
     */
    public void test_getByAddressLString$BLNetworkInterface()
            throws UnknownHostException {
        NetworkInterface nif = null;
        try {
            Inet6Address.getByAddress("123", null, nif);
            fail("should throw UnknownHostException");
        } catch (UnknownHostException uhe) {
            // expected
        }
        byte[] addr1 = { (byte) 127, 0, 0, 1 };
        try {
            Inet6Address.getByAddress("123", addr1, nif);
            fail("should throw UnknownHostException");
        } catch (UnknownHostException uhe) {
            // expected
        }
        byte[] addr2 = { (byte) 0xFE, (byte) 0x80, 0, 0, 0, 0, 0, 0, 0x02,
                0x11, 0x25, (byte) 0xFF, (byte) 0xFE, (byte) 0xF8, (byte)

                0x7C, (byte) 0xB2 };
        // should not throw any exception
        Inet6Address.getByAddress("123", addr2, nif);
    }

    /**
     * @throws UnknownHostException
     * java.net.Inet6Address#getScopeID()
     */
    public void test_getScopeID() throws UnknownHostException {
        Inet6Address v6ia;
        byte[] addr = { (byte) 0xFE, (byte) 0x80, 0, 0, 0, 0, 0, 0, 0x02, 0x11,
                0x25, (byte) 0xFF, (byte) 0xFE, (byte) 0xF8, (byte) 0x7C,
                (byte) 0xB2 };

        v6ia = Inet6Address.getByAddress("123", addr, 3);
        assertEquals(3, v6ia.getScopeId());

        v6ia = Inet6Address.getByAddress("123", addr, 0);
        assertEquals(0, v6ia.getScopeId());

        v6ia = Inet6Address.getByAddress("123", addr, -1);
        assertEquals(0, v6ia.getScopeId());
    }

    /**
     * java.net.Inet6Address#getScopedInterface()
     */
    public void test_getScopedInterface() throws UnknownHostException {
        byte[] addr = { (byte) 0xFE, (byte) 0x80, (byte) 0x09, (byte) 0xb5,
                (byte) 0x6b, (byte) 0xa4, 0, 0, 0, 0, 0, 0, (byte) 0x09,
                (byte) 0xb5, (byte) 0x6b, (byte) 0xa4 };
        Inet6Address v6Addr;
        v6Addr = Inet6Address.getByAddress("123", addr, null);
        assertNull(v6Addr.getScopedInterface());
    }


    int bytesToInt(byte bytes[], int start) {

        int byteMask = 255;
        int value = ((bytes[start + 3] & byteMask))
                | ((bytes[start + 2] & byteMask) << 8)
                | ((bytes[start + 1] & byteMask) << 16)
                | ((bytes[start] & byteMask) << 24);
        return value;

    }

    String byteArrayToHexString(byte bytes[], boolean leadingZeros) {

        String fullString = "";
        int times = bytes.length / 4;
        int intArray[] = new int[times];
        for (int i = 0; i < times; i++) {
            intArray[i] = bytesToInt(bytes, i * 4);
        }

        return intArrayToHexString(intArray, leadingZeros);
    }

    void intToBytes(int value, byte bytes[], int start) {

        int byteMask = 255;
        bytes[start + 3] = (byte) (value & byteMask);
        bytes[start + 2] = (byte) ((value >> 8) & byteMask);
        bytes[start + 1] = (byte) ((value >> 16) & byteMask);
        bytes[start] = (byte) ((value >> 24) & byteMask);
    }

    String intArrayToHexString(int ints[], boolean leadingZeros) {

        String fullString = "";
        String tempString;
        int intsLength = ints.length;
        for (int i = 0; i < intsLength; i++) {
            tempString = Integer.toHexString(ints[i]);
            while (tempString.length() < 4 && leadingZeros) {
                tempString = "0" + tempString;
            }
            if (i + 1 < intsLength) {
                tempString += ":";
            }
            fullString += tempString;
        }

        return fullString.toUpperCase();
    }

    // comparator for Inet6Address objects
    private static final SerializableAssert COMPARATOR = new SerializableAssert() {
        public void assertDeserialized(Serializable initial,
                Serializable deserialized) {

            Inet6Address initAddr = (Inet6Address) initial;
            Inet6Address desrAddr = (Inet6Address) deserialized;

            byte[] iaAddresss = initAddr.getAddress();
            byte[] deIAAddresss = desrAddr.getAddress();
            for (int i = 0; i < iaAddresss.length; i++) {
                assertEquals(iaAddresss[i], deIAAddresss[i]);
            }
            assertEquals(initAddr.getScopeId(), desrAddr.getScopeId());
            assertEquals(initAddr.getScopedInterface(), desrAddr
                    .getScopedInterface());
        }
    };

    /**
     * serialization/deserialization compatibility.
     */
    public void testSerializationSelf() throws Exception {

        byte[] localv6 = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };

        SerializationTest.verifySelf(InetAddress.getByAddress(localv6),
                COMPARATOR);
    }

    /**
     * serialization/deserialization compatibility with RI.
     */
    public void testSerializationCompatibility() throws Exception {

        byte[] localv6 = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };

        Object[] addresses = { InetAddress.getByAddress(localv6),
                // Regression for Harmony-1039: ser-form has
                // null interface name
                InetAddress.getByAddress(localv6) };

        SerializationTest.verifyGolden(this, addresses, COMPARATOR);
    }

    public void test_equals() throws Exception {
        InetAddress addr = InetAddress.getByName("239.191.255.255");
        assertTrue(addr.equals(addr));
        assertTrue(ipv6LoopbackIp.equals(ipv6Localhost));
        assertFalse(addr.equals(ipv6LoopbackIp));

        InetAddress addr3 = InetAddress.getByName("127.0.0");
        assertFalse(ipv6LoopbackIp.equals(addr3));
    }

    public void test_getHostAddress() throws Exception {
        assertEquals("::1", ipv6Localhost.getHostAddress());
        assertEquals("::1", InetAddress.getByName("::1").getHostAddress());

        InetAddress aAddr = InetAddress.getByName("224.0.0.0");
        assertEquals("224.0.0.0", aAddr.getHostAddress());

        aAddr = InetAddress.getByName("1");
        assertEquals("0.0.0.1", aAddr.getHostAddress());

        aAddr = InetAddress.getByName("1.1");
        assertEquals("1.0.0.1", aAddr.getHostAddress());

        aAddr = InetAddress.getByName("1.1.1");
        assertEquals("1.1.0.1", aAddr.getHostAddress());

        byte[] bAddr = { (byte) 0xFE, (byte) 0x80, 0, 0, 0, 0, 0, 0, 0x02, 0x11,
                0x25, (byte) 0xFF, (byte) 0xFE, (byte) 0xF8, (byte) 0x7C,
                (byte) 0xB2 };
        aAddr = Inet6Address.getByAddress(bAddr);
        String aString = aAddr.getHostAddress();
        assertTrue(aString.equals("fe80:0:0:0:211:25ff:fef8:7cb2") ||
                   aString.equals("fe80::211:25ff:fef8:7cb2"));

        byte[] cAddr = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        aAddr = Inet6Address.getByAddress(cAddr);
        assertEquals("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", aAddr.getHostAddress());

        byte[] dAddr = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        aAddr = Inet6Address.getByAddress(dAddr);
        aString = aAddr.getHostAddress();
        assertTrue(aString.equals("0:0:0:0:0:0:0:0") ||
                   aString.equals("::"));

        byte[] eAddr = { (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03,
                (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07,
                (byte) 0x08, (byte) 0x09, (byte) 0x0a, (byte) 0x0b,
                (byte) 0x0c, (byte) 0x0d, (byte) 0x0e, (byte) 0x0f};
        aAddr = Inet6Address.getByAddress(eAddr);
        assertEquals("1:203:405:607:809:a0b:c0d:e0f", aAddr.getHostAddress());

        byte[] fAddr = { (byte) 0x00, (byte) 0x10, (byte) 0x20, (byte) 0x30,
                (byte) 0x40, (byte) 0x50, (byte) 0x60, (byte) 0x70,
                (byte) 0x80, (byte) 0x90, (byte) 0xa0, (byte) 0xb0,
                (byte) 0xc0, (byte) 0xd0, (byte) 0xe0, (byte) 0xf0};
        aAddr = Inet6Address.getByAddress(fAddr);
        assertEquals("10:2030:4050:6070:8090:a0b0:c0d0:e0f0", aAddr.getHostAddress());
    }

    public void test_hashCode() throws Exception {
        InetAddress addr1 = InetAddress.getByName("1.1");
        InetAddress addr2 = InetAddress.getByName("1.1.1");
        assertFalse(addr1.hashCode() == addr2.hashCode());

        addr2 = InetAddress.getByName("1.0.0.1");
        assertTrue(addr1.hashCode() == addr2.hashCode());

        assertTrue(ipv6LoopbackIp.hashCode() == ipv6Localhost.hashCode());
    }

    public void test_toString() throws Exception {
        String validIPAddresses[] = { "::1.2.3.4", "::", "::", "1::0", "1::",
                "::1", "0", /* jdk1.5 accepts 0 as valid */
                "FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF",
                "FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:255.255.255.255",
                "0:0:0:0:0:0:0:0", "0:0:0:0:0:0:0.0.0.0" };

        String [] resultStrings = {"/0:0:0:0:0:0:102:304", "/0:0:0:0:0:0:0:0",
                "/0:0:0:0:0:0:0:0", "/1:0:0:0:0:0:0:0", "/1:0:0:0:0:0:0:0",
                "/0:0:0:0:0:0:0:1",
                "/0.0.0.0", "/ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
                "/ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", "/0:0:0:0:0:0:0:0",
                "/0:0:0:0:0:0:0:0"};

        for(int i = 0; i < validIPAddresses.length; i++) {
            InetAddress ia = InetAddress.getByName(validIPAddresses[i]);
            String result = ia.toString();
            assertNotNull(result);
            //assertEquals("toString method returns incorrect value: " +
            //        result, resultStrings[i], result);
        }
    }
}
