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

package org.apache.harmony.tests.java.net;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

public class MulticastSocketTest extends junit.framework.TestCase {

    private boolean atLeastTwoInterfaces = false;

    private NetworkInterface networkInterface1 = null;

    private NetworkInterface networkInterface2 = null;

    private NetworkInterface IPV6networkInterface1 = null;

    private static InetAddress lookup(String s) {
        try {
            return InetAddress.getByName(s);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    // These IP addresses aren't inherently "good" or "bad"; they're just used like that.
    // We use the "good" addresses for our actual group, and the "bad" addresses are for
    // a group that we won't actually set up.

    private static InetAddress GOOD_IPv4 = lookup("224.0.0.3");
    private static InetAddress BAD_IPv4 = lookup("224.0.0.4");

    private static InetAddress GOOD_IPv6 = lookup("ff05::7:7");
    private static InetAddress BAD_IPv6 = lookup("ff05::7:8");

    static class MulticastServer extends Thread {

        public final MulticastSocket ms;
        public final byte[] rbuf = new byte[512];
        public final DatagramPacket rdp;
        private final InetAddress groupAddr;
        private final NetworkInterface groupNI;

        private volatile boolean running = true;

        public MulticastServer(InetAddress anAddress, int aPort) throws java.io.IOException {
            rbuf[0] = -1;
            rdp = new DatagramPacket(rbuf, rbuf.length);
            ms = new MulticastSocket(aPort);
            ms.setSoTimeout(2000);

            groupAddr = anAddress;
            groupNI = null;

            ms.joinGroup(groupAddr);
        }

        public MulticastServer(InetAddress anAddress, int aPort,
                NetworkInterface netInterface) throws java.io.IOException {
            rbuf[0] = -1;
            rdp = new DatagramPacket(rbuf, rbuf.length);
            ms = new MulticastSocket(aPort);
            ms.setSoTimeout(2000);

            groupAddr = anAddress;
            groupNI = netInterface;

            ms.joinGroup(new InetSocketAddress(groupAddr, ms.getLocalPort()), groupNI);
        }

        public void run() {
            try {
                byte[] tmpbuf = new byte[512];
                DatagramPacket tmpPack = new DatagramPacket(tmpbuf, tmpbuf.length);

                while (running) {
                    try {
                        ms.receive(tmpPack);
                        System.arraycopy(tmpPack.getData(), 0, rdp.getData(), rdp.getOffset(),
                                tmpPack.getLength());
                        rdp.setLength(tmpPack.getLength());
                        rdp.setAddress(tmpPack.getAddress());
                        rdp.setPort(tmpPack.getPort());
                    } catch (InterruptedIOException e) {
                    }
                }
            } catch (java.io.IOException e) {
                fail();
            } finally {
                ms.close();
            }
        }

        public void stopServer() {
            running = false;
            try {
                ms.leaveGroup(groupAddr);
            } catch (IOException e) {
            }
        }
    }

    public void test_Constructor() throws IOException {
        // regression test for 497
        MulticastSocket s = new MulticastSocket();
        // regression test for Harmony-1162
        assertTrue(s.getReuseAddress());
    }

    public void test_ConstructorI() throws IOException {
        MulticastSocket orig = new MulticastSocket();
        int port = orig.getLocalPort();
        orig.close();
        MulticastSocket dup = null;
        try {
            dup = new MulticastSocket(port);
            // regression test for Harmony-1162
            assertTrue(dup.getReuseAddress());
        } catch (IOException e) {
            fail("duplicate binding not allowed: " + e);
        }
        if (dup != null) {
            dup.close();
        }
    }

    public void test_getInterface() throws Exception {
        // validate that we get the expected response when one was not set
        MulticastSocket mss = new MulticastSocket(0);
        // we expect an ANY address in this case
        assertTrue(mss.getInterface().isAnyLocalAddress());

        // validate that we get the expected response when we set via
        // setInterface
        Enumeration addresses = networkInterface1.getInetAddresses();
        if (addresses.hasMoreElements()) {
            InetAddress firstAddress = (InetAddress) addresses.nextElement();
            mss.setInterface(firstAddress);
            assertEquals("getNetworkInterface did not return interface set by setInterface", firstAddress, mss.getInterface());

            mss.close();
            mss = new MulticastSocket(0);
            mss.setNetworkInterface(networkInterface1);
            assertEquals("getInterface did not return interface set by setNetworkInterface", networkInterface1, NetworkInterface.getByInetAddress(mss.getInterface()));
        }

        mss.close();
    }

    public void test_getNetworkInterface() throws IOException {
        // validate that we get the expected response when one was not set
        MulticastSocket mss = new MulticastSocket(0);
        NetworkInterface theInterface = mss.getNetworkInterface();
        assertTrue("network interface returned wrong network interface when not set:" + theInterface,
                theInterface.getInetAddresses().hasMoreElements());
        InetAddress firstAddress = (InetAddress) theInterface.getInetAddresses().nextElement();
        // validate we the first address in the network interface is the ANY address
        assertTrue(firstAddress.isAnyLocalAddress());

        mss.setNetworkInterface(networkInterface1);
        assertEquals("getNetworkInterface did not return interface set by setNeworkInterface",
                networkInterface1, mss.getNetworkInterface());

        if (atLeastTwoInterfaces) {
            mss.setNetworkInterface(networkInterface2);
            assertEquals("getNetworkInterface did not return network interface set by second setNetworkInterface call",
                    networkInterface2, mss.getNetworkInterface());
        }
        mss.close();

        mss = new MulticastSocket(0);
        if (IPV6networkInterface1 != null) {
            mss.setNetworkInterface(IPV6networkInterface1);
            assertEquals("getNetworkInterface did not return interface set by setNeworkInterface",
                    IPV6networkInterface1, mss.getNetworkInterface());
        }

        // validate that we get the expected response when we set via setInterface
        mss = new MulticastSocket(0);
        Enumeration addresses = networkInterface1.getInetAddresses();
        if (addresses.hasMoreElements()) {
            firstAddress = (InetAddress) addresses.nextElement();
            mss.setInterface(firstAddress);
            assertEquals("getNetworkInterface did not return interface set by setInterface",
                    networkInterface1, mss.getNetworkInterface());
        }
        mss.close();
    }

    public void test_getTimeToLive() throws Exception {
        MulticastSocket mss = new MulticastSocket();
        mss.setTimeToLive(120);
        assertEquals("Returned incorrect 1st TTL", 120, mss.getTimeToLive());
        mss.setTimeToLive(220);
        assertEquals("Returned incorrect 2nd TTL", 220, mss.getTimeToLive());
    }

    public void test_getTTL() throws Exception {
        MulticastSocket mss = new MulticastSocket();
        mss.setTTL((byte) 120);
        assertEquals("Returned incorrect TTL", 120, mss.getTTL());
    }

    public void test_joinGroupLjava_net_InetAddress_IPv4() throws Exception {
        test_joinGroupLjava_net_InetAddress(GOOD_IPv4);
    }

    public void test_joinGroupLjava_net_InetAddress_IPv6() throws Exception {
        test_joinGroupLjava_net_InetAddress(GOOD_IPv6);
    }

    private void test_joinGroupLjava_net_InetAddress(InetAddress group) throws Exception {
        MulticastServer server = new MulticastServer(group, 0);
        server.start();
        Thread.sleep(1000);
        String msg = "Hello World";
        MulticastSocket mss = new MulticastSocket(server.ms.getLocalPort());
        DatagramPacket sdp = new DatagramPacket(msg.getBytes(), msg.length(), group,
                server.ms.getLocalPort());
        mss.send(sdp, (byte) 10);
        Thread.sleep(1000);
        String receivedMessage = new String(server.rdp.getData(), 0, server.rdp.getLength());
        assertEquals("Group member did not recv data", msg, receivedMessage);
        mss.close();
        server.stopServer();
    }

    public void test_joinGroup_null_null() throws Exception {
        MulticastSocket mss = new MulticastSocket(0);
        try {
            mss.joinGroup(null, null);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        mss.close();
    }

    public void test_joinGroup_non_multicast_address_IPv4() throws Exception {
        MulticastSocket mss = new MulticastSocket(0);
        try {
            mss.joinGroup(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), null);
            fail();
        } catch (IOException expected) {
        }
        mss.close();
    }

    public void test_joinGroup_non_multicast_address_IPv6() throws Exception {
        MulticastSocket mss = new MulticastSocket(0);
        try {
            mss.joinGroup(new InetSocketAddress(InetAddress.getByName("::1"), 0), null);
            fail();
        } catch (IOException expected) {
        }
        mss.close();
    }

    public void test_joinGroupLjava_net_SocketAddressLjava_net_NetworkInterface_IPv4() throws Exception {
        test_joinGroupLjava_net_SocketAddressLjava_net_NetworkInterface(GOOD_IPv4, BAD_IPv4);
    }

    public void test_joinGroupLjava_net_SocketAddressLjava_net_NetworkInterface_IPv6() throws Exception {
        test_joinGroupLjava_net_SocketAddressLjava_net_NetworkInterface(GOOD_IPv6, BAD_IPv6);
    }

    private void test_joinGroupLjava_net_SocketAddressLjava_net_NetworkInterface(InetAddress group, InetAddress group2) throws Exception {
        // Check that we can join a group using a null network interface.
        MulticastSocket mss = new MulticastSocket(0);
        SocketAddress groupSockAddr = new InetSocketAddress(group, mss.getLocalPort());

        mss.joinGroup(groupSockAddr, null);
        mss.setTimeToLive(2);
        Thread.sleep(1000);

        // set up the server and join the group on networkInterface1
        MulticastServer server = new MulticastServer(group, 0, networkInterface1);
        server.start();
        Thread.sleep(1000);
        String msg = "Hello World";
        DatagramPacket sdp = new DatagramPacket(msg.getBytes(), msg.length(), group,
                server.ms.getLocalPort());
        mss.setTimeToLive(2);
        mss.send(sdp);
        Thread.sleep(1000);
        // now validate that we received the data as expected
        assertEquals("Group member did not recv data", msg, new String(server.rdp.getData(), 0, server.rdp.getLength()));
        server.stopServer();
        mss.close();

        server = new MulticastServer(group, 0, networkInterface1);
        server.start();
        Thread.sleep(1000);

        mss = new MulticastSocket(0);
        mss.setTimeToLive(10);
        msg = "Hello World - Different Group";
        sdp = new DatagramPacket(msg.getBytes(), msg.length(), group2, server.ms.getLocalPort());
        mss.send(sdp);
        Thread.sleep(1000);
        assertFalse("Group member received data when sent on different group: ",
                new String(server.rdp.getData(), 0, server.rdp.getLength()).equals(msg));
        server.stopServer();
        mss.close();
    }

    public void test_joinGroupLjava_net_SocketAddressLjava_net_NetworkInterface() throws Exception {
        // if there is more than one network interface then check that
        // we can join on specific interfaces and that we only receive
        // if data is received on that interface
        if (!atLeastTwoInterfaces) {
            return;
        }
        // set up server on first interfaces
        NetworkInterface loopbackInterface = NetworkInterface.getByInetAddress(
                InetAddress.getByName("127.0.0.1"));

        boolean anyLoop = networkInterface1.equals(loopbackInterface) ||
                networkInterface2.equals(loopbackInterface);

        ArrayList<NetworkInterface> realInterfaces = new ArrayList<NetworkInterface>();
        Enumeration<NetworkInterface> theInterfaces = NetworkInterface.getNetworkInterfaces();
        while (theInterfaces.hasMoreElements()) {
            NetworkInterface thisInterface = theInterfaces.nextElement();
            if (thisInterface.getInetAddresses().hasMoreElements()) {
                realInterfaces.add(thisInterface);
            }
        }

        for (int i = 0; i < realInterfaces.size(); i++) {
            NetworkInterface thisInterface = realInterfaces.get(i);

            // get the first address on the interface

            // start server which is joined to the group and has
            // only asked for packets on this interface
            Enumeration<InetAddress> addresses = thisInterface.getInetAddresses();

            NetworkInterface sendingInterface = null;
            InetAddress group = null;
            if (addresses.hasMoreElements()) {
                InetAddress firstAddress = addresses.nextElement();
                if (firstAddress instanceof Inet4Address) {
                    group = InetAddress.getByName("224.0.0.4");
                    if (anyLoop) {
                        if (networkInterface1.equals(loopbackInterface)) {
                            sendingInterface = networkInterface2;
                        } else {
                            sendingInterface = networkInterface1;
                        }
                    } else {
                        if (i == 1) {
                            sendingInterface = networkInterface2;
                        } else {
                            sendingInterface = networkInterface1;
                        }
                    }
                } else {
                    // if this interface only seems to support IPV6 addresses
                    group = InetAddress.getByName("FF01:0:0:0:0:0:2:8001");
                    sendingInterface = IPV6networkInterface1;
                }
            }


            MulticastServer server = new MulticastServer(group, 0, thisInterface);
            server.start();
            Thread.sleep(1000);

            // Now send out a package on interface
            // networkInterface 1. We should
            // only see the packet if we send it on interface 1
            MulticastSocket mss = new MulticastSocket(0);
            mss.setNetworkInterface(sendingInterface);
            String msg = "Hello World - Again" + thisInterface.getName();
            DatagramPacket sdp = new DatagramPacket(msg.getBytes(), msg.length(), group,
                    server.ms.getLocalPort());
            mss.send(sdp);
            Thread.sleep(1000);
            if (thisInterface.equals(sendingInterface)) {
                assertEquals("Group member did not recv data when bound on specific interface",
                        msg, new String(server.rdp.getData(), 0, server.rdp.getLength()));
            } else {
                assertFalse("Group member received data on other interface when only asked for it on one interface: ",
                        new String(server.rdp.getData(), 0, server.rdp.getLength()).equals(msg));
            }

            server.stopServer();
            mss.close();
        }
    }

    public void test_joinGroupLjava_net_SocketAddressLjava_net_NetworkInterface_multiple_joins_IPv4() throws Exception {
        test_joinGroupLjava_net_SocketAddressLjava_net_NetworkInterface_multiple_joins(GOOD_IPv4);
    }

    public void test_joinGroupLjava_net_SocketAddressLjava_net_NetworkInterface_multiple_joins_IPv6() throws Exception {
        test_joinGroupLjava_net_SocketAddressLjava_net_NetworkInterface_multiple_joins(GOOD_IPv6);
    }

    private void test_joinGroupLjava_net_SocketAddressLjava_net_NetworkInterface_multiple_joins(InetAddress group) throws Exception {
        // validate that we can join the same address on two
        // different interfaces but not on the same interface
        MulticastSocket mss = new MulticastSocket(0);
        SocketAddress groupSockAddr = new InetSocketAddress(group, mss.getLocalPort());
        mss.joinGroup(groupSockAddr, networkInterface1);
        mss.joinGroup(groupSockAddr, networkInterface2);
        try {
            mss.joinGroup(groupSockAddr, networkInterface1);
            fail("Did not get expected exception when joining for second time on same interface");
        } catch (IOException e) {
        }
        mss.close();
    }

    public void test_leaveGroupLjava_net_InetAddress_IPv4() throws Exception {
        test_leaveGroupLjava_net_InetAddress(GOOD_IPv4);
    }

    public void test_leaveGroupLjava_net_InetAddress_IPv6() throws Exception {
        test_leaveGroupLjava_net_InetAddress(GOOD_IPv6);
    }

    private void test_leaveGroupLjava_net_InetAddress(InetAddress group) throws Exception {
        String msg = "Hello World";
        MulticastSocket mss = new MulticastSocket(0);
        DatagramPacket sdp = new DatagramPacket(msg.getBytes(), msg.length(), group,
                mss.getLocalPort());
        mss.send(sdp, (byte) 10);
        try {
            // Try to leave a group we didn't join.
            mss.leaveGroup(group);
            fail();
        } catch (IOException expected) {
        }
        mss.close();
    }

    public void test_leaveGroup_null_null() throws Exception {
        MulticastSocket mss = new MulticastSocket(0);
        try {
            mss.leaveGroup(null, null);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        mss.close();
    }

    public void test_leaveGroup_non_multicast_address_IPv4() throws Exception {
        MulticastSocket mss = new MulticastSocket(0);
        try {
            mss.leaveGroup(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), null);
            fail();
        } catch (IOException expected) {
        }
        mss.close();
    }

    public void test_leaveGroup_non_multicast_address_IPv6() throws Exception {
        MulticastSocket mss = new MulticastSocket(0);
        try {
            mss.leaveGroup(new InetSocketAddress(InetAddress.getByName("::1"), 0), null);
            fail();
        } catch (IOException expected) {
        }
        mss.close();
    }

    public void test_leaveGroupLjava_net_SocketAddressLjava_net_NetworkInterface_IPv4() throws Exception {
        test_leaveGroupLjava_net_SocketAddressLjava_net_NetworkInterface(GOOD_IPv4, BAD_IPv4);
    }

    public void test_leaveGroupLjava_net_SocketAddressLjava_net_NetworkInterface_IPv6() throws Exception {
        test_leaveGroupLjava_net_SocketAddressLjava_net_NetworkInterface(GOOD_IPv6, BAD_IPv6);
    }

    private void test_leaveGroupLjava_net_SocketAddressLjava_net_NetworkInterface(InetAddress group, InetAddress group2) throws Exception {
        String msg = null;
        SocketAddress groupSockAddr = null;
        SocketAddress groupSockAddr2 = null;

        MulticastSocket mss = new MulticastSocket(0);
        groupSockAddr = new InetSocketAddress(group, mss.getLocalPort());
        mss.joinGroup(groupSockAddr, null);
        mss.leaveGroup(groupSockAddr, null);
        try {
            mss.leaveGroup(groupSockAddr, null);
            fail("Did not get exception when trying to leave group that was already left");
        } catch (IOException expected) {
        }

        groupSockAddr2 = new InetSocketAddress(group2, mss.getLocalPort());
        mss.joinGroup(groupSockAddr, networkInterface1);
        try {
            mss.leaveGroup(groupSockAddr2, networkInterface1);
            fail("Did not get exception when trying to leave group that was never joined");
        } catch (IOException expected) {
        }

        mss.leaveGroup(groupSockAddr, networkInterface1);
        if (atLeastTwoInterfaces) {
            mss.joinGroup(groupSockAddr, networkInterface1);
            try {
                mss.leaveGroup(groupSockAddr, networkInterface2);
                fail("Did not get exception when trying to leave group on wrong interface " +
                        "joined on [" + networkInterface1 + "] left on [" + networkInterface2 + "]");
            } catch (IOException expected) {
            }
        }
    }

    public void test_sendLjava_net_DatagramPacketB_IPv4() throws Exception {
        test_sendLjava_net_DatagramPacketB(GOOD_IPv4);
    }

    public void test_sendLjava_net_DatagramPacketB_IPv6() throws Exception {
        test_sendLjava_net_DatagramPacketB(GOOD_IPv6);
    }

    private void test_sendLjava_net_DatagramPacketB(InetAddress group) throws Exception {
        String msg = "Hello World";
        MulticastSocket mss = new MulticastSocket(0);
        MulticastServer server = new MulticastServer(group, mss.getLocalPort());
        server.start();
        Thread.sleep(200);
        DatagramPacket sdp = new DatagramPacket(msg.getBytes(), msg.length(), group, mss.getLocalPort());
        mss.send(sdp, (byte) 10);
        Thread.sleep(1000);
        mss.close();
        byte[] data = server.rdp.getData();
        int length = server.rdp.getLength();
        assertEquals("Failed to send data. Received " + length, msg, new String(data, 0, length));
        server.stopServer();
    }

    public void test_setInterfaceLjava_net_InetAddress() throws Exception {
        MulticastSocket mss = new MulticastSocket();
        mss.setInterface(InetAddress.getLocalHost());
        InetAddress theInterface = mss.getInterface();
        // under IPV6 we are not guarrenteed to get the same address back as
        // the address, all we should be guaranteed is that we get an
        // address on the same interface
        if (theInterface instanceof Inet6Address) {
            assertTrue("Failed to return correct interface IPV6", NetworkInterface.getByInetAddress(mss.getInterface()).equals(NetworkInterface.getByInetAddress(theInterface)));
        } else {
            assertTrue("Failed to return correct interface IPV4 got:" + mss.getInterface() + " excpeted: " + InetAddress.getLocalHost(), mss.getInterface().equals(InetAddress.getLocalHost()));
        }
        mss.close();
    }

    public void test_setInterface_unbound_address_IPv4() throws Exception {
        test_setInterface_unbound_address(GOOD_IPv4);
    }

    public void test_setInterface_unbound_address_IPv6() throws Exception {
        test_setInterface_unbound_address(GOOD_IPv6);
    }

    // Regression test for Harmony-2410
    private void test_setInterface_unbound_address(InetAddress address) throws Exception {
        MulticastSocket mss = new MulticastSocket();
        try {
            mss.setInterface(address);
            fail();
        } catch (SocketException expected) {
        }
        mss.close();
    }

    public void test_setNetworkInterfaceLjava_net_NetworkInterface_null() throws Exception {
        // validate that null interface is handled ok
        MulticastSocket mss = new MulticastSocket();
        try {
            mss.setNetworkInterface(null);
            fail("No socket exception when we set then network interface with NULL");
        } catch (SocketException ex) {
        }
        mss.close();
    }

    public void test_setNetworkInterfaceLjava_net_NetworkInterface_round_trip() throws Exception {
        // validate that we can get and set the interface
        MulticastSocket mss = new MulticastSocket();
        mss.setNetworkInterface(networkInterface1);
        assertEquals("Interface did not seem to be set by setNeworkInterface", networkInterface1, mss.getNetworkInterface());
        mss.close();
    }

    public void test_setNetworkInterfaceLjava_net_NetworkInterface_IPv4() throws Exception {
        test_setNetworkInterfaceLjava_net_NetworkInterface(GOOD_IPv4);
    }

    public void test_setNetworkInterfaceLjava_net_NetworkInterface_IPv6() throws Exception {
        test_setNetworkInterfaceLjava_net_NetworkInterface(GOOD_IPv6);
    }

    private void test_setNetworkInterfaceLjava_net_NetworkInterface(InetAddress group) throws IOException, InterruptedException {
        // set up the server and join the group
        Enumeration theInterfaces = NetworkInterface.getNetworkInterfaces();
        while (theInterfaces.hasMoreElements()) {
            NetworkInterface thisInterface = (NetworkInterface) theInterfaces.nextElement();
            if (thisInterface.getInetAddresses().hasMoreElements() && thisInterface.isUp()) {
                if ((!(thisInterface.getInetAddresses().nextElement()).isLoopbackAddress())) {
                    MulticastServer server = new MulticastServer(group, 0);
                    server.start();
                    // give the server some time to start up
                    Thread.sleep(1000);

                    // Send the packets on a particular interface. The
                    // source address in the received packet
                    // should be one of the addresses for the interface
                    // set
                    MulticastSocket mss = new MulticastSocket(0);
                    mss.setNetworkInterface(thisInterface);
                    String msg = thisInterface.getName();
                    byte theBytes[] = msg.getBytes();
                    DatagramPacket sdp = new DatagramPacket(theBytes, theBytes.length, group,
                            server.ms.getLocalPort());
                    mss.send(sdp);
                    Thread.sleep(1000);
                    String receivedMessage = new String(server.rdp.getData(), 0, server.rdp.getLength());
                    assertEquals("Group member did not recv data sent on a specific interface", msg, receivedMessage);
                    // stop the server
                    server.stopServer();
                    mss.close();
                }
            }
        }
    }

    public void test_setTimeToLiveI() throws Exception {
        MulticastSocket mss = new MulticastSocket();
        mss.setTimeToLive(120);
        assertEquals("Returned incorrect 1st TTL", 120, mss.getTimeToLive());
        mss.setTimeToLive(220);
        assertEquals("Returned incorrect 2nd TTL", 220, mss.getTimeToLive());
        mss.close();
    }

    public void test_setTTLB() throws Exception {
        MulticastSocket mss = new MulticastSocket();
        mss.setTTL((byte) 120);
        assertEquals("Failed to set TTL", 120, mss.getTTL());
        mss.close();
    }

    public void test_ConstructorLjava_net_SocketAddress() throws Exception {
        MulticastSocket ms = new MulticastSocket((SocketAddress) null);
        assertTrue("should not be bound", !ms.isBound() && !ms.isClosed() && !ms.isConnected());
        ms.bind(null);
        assertTrue("should be bound", ms.isBound() && !ms.isClosed() && !ms.isConnected());
        ms.close();
        assertTrue("should be closed", ms.isClosed());
        ms = new MulticastSocket(0);
        assertTrue("should be bound", ms.isBound() && !ms.isClosed() && !ms.isConnected());
        ms.close();
        assertTrue("should be closed", ms.isClosed());
        ms = new MulticastSocket(0);
        assertTrue("should be bound", ms.isBound() && !ms.isClosed() && !ms.isConnected());
        ms.close();
        assertTrue("should be closed", ms.isClosed());
        try {
            ms = new MulticastSocket(new InetSocketAddress("unresolvedname", 31415));
            fail();
        } catch (IOException expected) {
        }

        // regression test for Harmony-1162
        InetSocketAddress addr = new InetSocketAddress("0.0.0.0", 0);
        MulticastSocket s = new MulticastSocket(addr);
        assertTrue(s.getReuseAddress());
    }

    public void test_getLoopbackMode() throws Exception {
        MulticastSocket ms = new MulticastSocket(null);
        assertTrue("should not be bound", !ms.isBound() && !ms.isClosed() && !ms.isConnected());
        ms.getLoopbackMode();
        assertTrue("should not be bound", !ms.isBound() && !ms.isClosed() && !ms.isConnected());
        ms.close();
        assertTrue("should be closed", ms.isClosed());
    }

    public void test_setLoopbackModeZ() throws Exception {
        MulticastSocket ms = new MulticastSocket();
        ms.setLoopbackMode(true);
        assertTrue("loopback should be true", ms.getLoopbackMode());
        ms.setLoopbackMode(false);
        assertTrue("loopback should be false", !ms.getLoopbackMode());
        ms.close();
        assertTrue("should be closed", ms.isClosed());
    }

    public void test_setLoopbackModeSendReceive_IPv4() throws Exception {
        test_setLoopbackModeSendReceive(GOOD_IPv4);
    }

    public void test_setLoopbackModeSendReceive_IPv6() throws Exception {
        test_setLoopbackModeSendReceive(GOOD_IPv6);
    }

    private void test_setLoopbackModeSendReceive(InetAddress group) throws IOException {
        final String message = "Hello, world!";

        // test send receive
        MulticastSocket socket = new MulticastSocket(0);
        socket.setLoopbackMode(false); // false indicates doing loop back
        socket.joinGroup(group);

        // send the datagram
        byte[] sendData = message.getBytes();
        DatagramPacket sendDatagram = new DatagramPacket(sendData, 0, sendData.length,
                new InetSocketAddress(group, socket.getLocalPort()));
        socket.send(sendDatagram);

        // receive the datagram
        byte[] recvData = new byte[100];
        DatagramPacket recvDatagram = new DatagramPacket(recvData, recvData.length);
        socket.setSoTimeout(5000); // prevent eternal block in
        socket.receive(recvDatagram);
        String recvMessage = new String(recvData, 0, recvDatagram.getLength());
        assertEquals(message, recvMessage);
        socket.close();
    }

    public void test_setReuseAddressZ() throws Exception {
        // test case were we set it to false
        MulticastSocket theSocket1 = null;
        MulticastSocket theSocket2 = null;
        try {
            theSocket1 = new MulticastSocket(null);
            theSocket2 = new MulticastSocket(null);
            theSocket1.setReuseAddress(false);
            theSocket2.setReuseAddress(false);
            InetSocketAddress addr = new InetSocketAddress(Inet4Address.getLocalHost(), 0);
            theSocket1.bind(addr);
            addr = new InetSocketAddress(Inet4Address.getLocalHost(), theSocket1.getLocalPort());
            theSocket2.bind(addr);
            fail("No exception when trying to connect to do duplicate socket bind with re-useaddr set to false");
        } catch (BindException expected) {
        }
        if (theSocket1 != null) {
            theSocket1.close();
        }
        if (theSocket2 != null) {
            theSocket2.close();
        }

        // test case were we set it to true
        theSocket1 = new MulticastSocket(null);
        theSocket2 = new MulticastSocket(null);
        theSocket1.setReuseAddress(true);
        theSocket2.setReuseAddress(true);
        InetSocketAddress addr = new InetSocketAddress(Inet4Address.getLocalHost(), 0);
        theSocket1.bind(addr);
        addr = new InetSocketAddress(Inet4Address.getLocalHost(), theSocket1.getLocalPort());
        theSocket2.bind(addr);

        if (theSocket1 != null) {
            theSocket1.close();
        }
        if (theSocket2 != null) {
            theSocket2.close();
        }

        // test the default case which we expect to be
        // the same on all platforms
        theSocket1 = new MulticastSocket(null);
        theSocket2 = new MulticastSocket(null);
        addr = new InetSocketAddress(Inet4Address.getLocalHost(), 0);
        theSocket1.bind(addr);
        addr = new InetSocketAddress(Inet4Address.getLocalHost(), theSocket1.getLocalPort());
        theSocket2.bind(addr);
        if (theSocket1 != null) {
            theSocket1.close();
        }
        if (theSocket2 != null) {
            theSocket2.close();
        }
    }

    @Override
    protected void setUp() throws Exception {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        // only consider interfaces that have addresses associated with them.
        // Otherwise tests don't work so well
        if ( interfaces != null) {
            boolean atLeastOneInterface = false;
            while ( interfaces.hasMoreElements() && (atLeastOneInterface == false)) {
                networkInterface1 =  interfaces.nextElement();
                if (isUpAndHasAddresses(networkInterface1)) {
                    atLeastOneInterface = true;
                }
            }

            assertTrue(atLeastOneInterface);

            atLeastTwoInterfaces = false;
            if ( interfaces.hasMoreElements()) {
                while ( interfaces.hasMoreElements() && (atLeastTwoInterfaces == false)) {
                    networkInterface2 =  interfaces.nextElement();
                    if (isUpAndHasAddresses(networkInterface2)) {
                        atLeastTwoInterfaces = true;
                    }
                }
            }

            // first the first interface that supports IPV6 if one exists
             interfaces = NetworkInterface.getNetworkInterfaces();

            boolean found = false;
            while ( interfaces.hasMoreElements() && !found) {
                NetworkInterface nextInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = nextInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    final InetAddress nextAddress = addresses.nextElement();
                    if (nextAddress instanceof Inet6Address) {
                        IPV6networkInterface1 = nextInterface;
                        found = true;
                        break;
                    }
                }
            }
        }
    }

    private static boolean isUpAndHasAddresses(NetworkInterface iface) throws IOException {
        return iface.isUp() && iface.getInetAddresses().hasMoreElements();
    }
}
