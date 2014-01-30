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

package libcore.java.nio.channels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.Enumeration;
import java.util.Set;

public class ServerSocketChannelTest extends junit.framework.TestCase {
    // http://code.google.com/p/android/issues/detail?id=16579
    public void testNonBlockingAccept() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        try {
            ssc.configureBlocking(false);
            ssc.bind(null);
            // Should return immediately, since we're non-blocking.
            assertNull(ssc.accept());
        } finally {
            ssc.close();
        }
    }

    /** Checks the state of the ServerSocketChannel and associated ServerSocket after open() */
    public void test_open_initialState() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        try {
            assertNull(ssc.getLocalAddress());

            ServerSocket socket = ssc.socket();
            assertFalse(socket.isBound());
            assertFalse(socket.isClosed());
            assertEquals(-1, socket.getLocalPort());
            assertNull(socket.getLocalSocketAddress());
            assertNull(socket.getInetAddress());
            assertTrue(socket.getReuseAddress());

            assertSame(ssc, socket.getChannel());
        } finally {
            ssc.close();
        }
    }

    public void test_bind_unresolvedAddress() throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        try {
            ssc.bind(new InetSocketAddress("unresolvedname", 31415));
            fail();
        } catch (UnresolvedAddressException expected) {
        }

        assertNull(ssc.getLocalAddress());
        assertTrue(ssc.isOpen());

        ssc.close();
    }

    public void test_bind_nullBindsToAll() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(null);
        InetSocketAddress boundAddress = (InetSocketAddress) ssc.getLocalAddress();
        assertTrue(boundAddress.getAddress().isAnyLocalAddress());
        assertFalse(boundAddress.getAddress().isLinkLocalAddress());
        assertFalse(boundAddress.getAddress().isLoopbackAddress());

        // Attempt to connect to the "any" address.
        assertTrue(canConnect(boundAddress));

        // Go through all local IPs and try to connect to each in turn - all should succeed.
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface nic = interfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = nic.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetSocketAddress address =
                        new InetSocketAddress(inetAddresses.nextElement(), boundAddress.getPort());
                assertTrue(canConnect(address));
            }
        }

        ssc.close();
    }

    public void test_bind_loopback() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        InetSocketAddress boundAddress = (InetSocketAddress) ssc.getLocalAddress();
        assertFalse(boundAddress.getAddress().isAnyLocalAddress());
        assertFalse(boundAddress.getAddress().isLinkLocalAddress());
        assertTrue(boundAddress.getAddress().isLoopbackAddress());

        // Attempt to connect to the "loopback" address. Note: There can be several loopback
        // addresses, such as 127.0.0.1 (IPv4) and 0:0:0:0:0:0:0:1 (IPv6) and only one will be
        // bound.
        InetSocketAddress loopbackAddress =
                new InetSocketAddress(InetAddress.getLoopbackAddress(), boundAddress.getPort());
        assertTrue(canConnect(loopbackAddress));

        // Go through all local IPs and try to connect to each in turn - all should fail except
        // for the loopback.
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface nic = interfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = nic.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetSocketAddress address =
                        new InetSocketAddress(inetAddresses.nextElement(), boundAddress.getPort());
                if (!address.equals(loopbackAddress)) {
                    assertFalse(canConnect(address));
                }
            }
        }

        ssc.close();
    }

    private static boolean canConnect(InetSocketAddress address) {
        try {
            SocketChannel socketChannel = SocketChannel.open(address);
            socketChannel.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void test_supportedOptions() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        Set<SocketOption<?>> options = ssc.supportedOptions();

        // Probe some values. This is not intended to be complete.
        assertTrue(options.contains(StandardSocketOptions.SO_REUSEADDR));
        assertFalse(options.contains(StandardSocketOptions.IP_MULTICAST_TTL));
    }

    public void test_getOption_unsupportedOption() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        try {
            ssc.getOption(StandardSocketOptions.IP_MULTICAST_TTL);
            fail();
        } catch (UnsupportedOperationException expected) {}

        ssc.close();
    }

    public void test_getOption_afterClose() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.close();

        try {
            ssc.getOption(StandardSocketOptions.SO_RCVBUF);
            fail();
        } catch (ClosedChannelException expected) {}
    }

    public void test_setOption_afterClose() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.close();

        try {
            ssc.setOption(StandardSocketOptions.SO_RCVBUF, 1234);
            fail();
        } catch (ClosedChannelException expected) {}
    }

    public void test_getOption_SO_RCVBUF_defaults() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();

        int value = ssc.getOption(StandardSocketOptions.SO_RCVBUF);
        assertTrue(value > 0);
        assertEquals(value, ssc.socket().getReceiveBufferSize());

        ssc.close();
    }

    public void test_setOption_SO_RCVBUF_afterOpen() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();

        trySetReceiveBufferSizeOption(ssc);

        ssc.close();
    }

    private static void trySetReceiveBufferSizeOption(ServerSocketChannel ssc) throws IOException {
        int initialValue = ssc.getOption(StandardSocketOptions.SO_RCVBUF);
        try {
            ssc.setOption(StandardSocketOptions.SO_RCVBUF, -1);
            fail();
        } catch (IllegalArgumentException expected) {}
        int actualValue = ssc.getOption(StandardSocketOptions.SO_RCVBUF);
        assertEquals(initialValue, actualValue);
        assertEquals(initialValue, ssc.socket().getReceiveBufferSize());

        int newBufferSize = initialValue - 1;
        ssc.setOption(StandardSocketOptions.SO_RCVBUF, newBufferSize);
        actualValue = ssc.getOption(StandardSocketOptions.SO_RCVBUF);
        // The Linux Kernel actually doubles the value it is given and may choose to ignore it.
        // This assertion may be brittle.
        assertTrue(actualValue != initialValue);
        assertEquals(actualValue, ssc.socket().getReceiveBufferSize());
    }

    public void test_getOption_SO_REUSEADDR_defaults() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();

        boolean value = ssc.getOption(StandardSocketOptions.SO_REUSEADDR);
        assertTrue(value);
        assertTrue(ssc.socket().getReuseAddress());

        ssc.close();
    }

    public void test_setOption_SO_REUSEADDR_afterOpen() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();

        boolean initialValue = ssc.getOption(StandardSocketOptions.SO_REUSEADDR);
        ssc.setOption(StandardSocketOptions.SO_REUSEADDR, !initialValue);
        assertEquals(!initialValue, (boolean) ssc.getOption(StandardSocketOptions.SO_REUSEADDR));
        assertEquals(!initialValue, ssc.socket().getReuseAddress());

        ssc.close();
    }

}
