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

package libcore.java.nio.channels;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.util.Enumeration;
import java.util.Set;

public class DatagramChannelTest extends junit.framework.TestCase {
    public void test_read_intoReadOnlyByteArrays() throws Exception {
        ByteBuffer readOnly = ByteBuffer.allocate(1).asReadOnlyBuffer();
        DatagramSocket ds = new DatagramSocket(0);
        DatagramChannel dc = DatagramChannel.open();
        dc.connect(ds.getLocalSocketAddress());
        try {
            dc.read(readOnly);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            dc.read(new ByteBuffer[] { readOnly });
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            dc.read(new ByteBuffer[] { readOnly }, 0, 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    // http://code.google.com/p/android/issues/detail?id=16579
    public void testNonBlockingRecv() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        try {
            dc.configureBlocking(false);
            dc.bind(null);
            // Should return immediately, since we're non-blocking.
            assertNull(dc.receive(ByteBuffer.allocate(2048)));
        } finally {
            dc.close();
        }
    }

    public void testInitialState() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        try {
            assertNull(dc.getLocalAddress());

            DatagramSocket socket = dc.socket();
            assertFalse(socket.isBound());
            assertFalse(socket.getBroadcast());
            assertFalse(socket.isClosed());
            assertFalse(socket.isConnected());
            assertEquals(0, socket.getLocalPort());
            assertTrue(socket.getLocalAddress().isAnyLocalAddress());
            assertNull(socket.getLocalSocketAddress());
            assertNull(socket.getInetAddress());
            assertEquals(-1, socket.getPort());
            assertNull(socket.getRemoteSocketAddress());
            assertFalse(socket.getReuseAddress());

            assertSame(dc, socket.getChannel());
        } finally {
            dc.close();
        }
    }

    public void test_supportedOptions() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        Set<SocketOption<?>> options = dc.supportedOptions();

        // Probe some values. This is not intended to be complete.
        assertTrue(options.contains(StandardSocketOptions.SO_REUSEADDR));
        assertFalse(options.contains(StandardSocketOptions.TCP_NODELAY));
    }

    public void test_getOption_unsupportedOption() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        try {
            dc.getOption(StandardSocketOptions.TCP_NODELAY);
            fail();
        } catch (UnsupportedOperationException expected) {}

        dc.close();
    }

    public void test_getOption_afterClose() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        dc.close();

        try {
            dc.getOption(StandardSocketOptions.SO_RCVBUF);
            fail();
        } catch (ClosedChannelException expected) {}
    }

    public void test_setOption_afterClose() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        dc.close();

        try {
            dc.setOption(StandardSocketOptions.SO_RCVBUF, 1234);
            fail();
        } catch (ClosedChannelException expected) {}
    }

    public void test_getOption_SO_RCVBUF_defaults() throws Exception {
        DatagramChannel dc = DatagramChannel.open();

        int value = dc.getOption(StandardSocketOptions.SO_RCVBUF);
        assertTrue(value > 0);
        assertEquals(value, dc.socket().getReceiveBufferSize());

        dc.close();
    }

    public void test_setOption_SO_RCVBUF_afterOpen() throws Exception {
        DatagramChannel dc = DatagramChannel.open();

        trySetReceiveBufferSizeOption(dc);

        dc.close();
    }

    private static void trySetReceiveBufferSizeOption(DatagramChannel dc) throws IOException {
        int initialValue = dc.getOption(StandardSocketOptions.SO_RCVBUF);
        try {
            dc.setOption(StandardSocketOptions.SO_RCVBUF, -1);
            fail();
        } catch (IllegalArgumentException expected) {}
        int actualValue = dc.getOption(StandardSocketOptions.SO_RCVBUF);
        assertEquals(initialValue, actualValue);
        assertEquals(initialValue, dc.socket().getReceiveBufferSize());

        int newBufferSize = initialValue - 1;
        dc.setOption(StandardSocketOptions.SO_RCVBUF, newBufferSize);
        actualValue = dc.getOption(StandardSocketOptions.SO_RCVBUF);
        // The Linux Kernel actually doubles the value it is given and may choose to ignore it.
        // This assertion may be brittle.
        assertTrue(actualValue != initialValue);
        assertEquals(actualValue, dc.socket().getReceiveBufferSize());
    }

    public void test_getOption_SO_SNDBUF_defaults() throws Exception {
        DatagramChannel dc = DatagramChannel.open();

        int value = dc.getOption(StandardSocketOptions.SO_SNDBUF);
        assertTrue(value > 0);
        assertEquals(value, dc.socket().getSendBufferSize());

        dc.close();
    }

    public void test_setOption_SO_SNDBUF_afterOpen() throws Exception {
        DatagramChannel dc = DatagramChannel.open();

        trySetSendBufferSizeOption(dc);

        dc.close();
    }

    private static void trySetSendBufferSizeOption(DatagramChannel dc) throws IOException {
        int initialValue = dc.getOption(StandardSocketOptions.SO_SNDBUF);
        try {
            dc.setOption(StandardSocketOptions.SO_SNDBUF, -1);
            fail();
        } catch (IllegalArgumentException expected) {}
        int actualValue = dc.getOption(StandardSocketOptions.SO_SNDBUF);
        assertEquals(initialValue, actualValue);
        assertEquals(initialValue, dc.socket().getSendBufferSize());

        int newBufferSize = initialValue - 1;
        dc.setOption(StandardSocketOptions.SO_SNDBUF, newBufferSize);
        actualValue = dc.getOption(StandardSocketOptions.SO_SNDBUF);
        // The Linux Kernel actually doubles the value it is given and may choose to ignore it.
        // This assertion may be brittle.
        assertTrue(actualValue != initialValue);
        assertEquals(actualValue, dc.socket().getSendBufferSize());
    }

    public void test_getOption_IP_MULTICAST_IF_defaults() throws Exception {
        DatagramChannel dc = DatagramChannel.open();

        NetworkInterface networkInterface = dc.getOption(StandardSocketOptions.IP_MULTICAST_IF);
        assertNull(networkInterface);

        dc.close();
    }

    public void test_getOption_IP_MULTICAST_IF_nullCheck() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        try {
            dc.setOption(StandardSocketOptions.IP_MULTICAST_IF, null);
            fail();
        } catch (IllegalArgumentException expected) {}

        dc.close();
    }

    public void test_setOption_IP_MULTICAST_IF_afterOpen() throws Exception {
        DatagramChannel dc = DatagramChannel.open();

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        assertTrue(networkInterfaces.hasMoreElements());
        while (networkInterfaces.hasMoreElements()) {
            trySetNetworkInterfaceOption(dc, networkInterfaces.nextElement());
        }

        dc.close();
    }

    public void test_setOption_IP_MULTICAST_IF_afterBind() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        dc.bind(new InetSocketAddress(Inet4Address.getLoopbackAddress(), 0));

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        assertTrue(networkInterfaces.hasMoreElements());
        while (networkInterfaces.hasMoreElements()) {
            trySetNetworkInterfaceOption(dc, networkInterfaces.nextElement());
        }

        dc.close();
    }

    private static void trySetNetworkInterfaceOption(
            DatagramChannel dc, NetworkInterface networkInterface) throws IOException {

        NetworkInterface initialValue = dc.getOption(StandardSocketOptions.IP_MULTICAST_IF);
        try {
            dc.setOption(StandardSocketOptions.IP_MULTICAST_IF, null);
            fail();
        } catch (IllegalArgumentException expected) {}
        assertEquals(initialValue, dc.getOption(StandardSocketOptions.IP_MULTICAST_IF));

        dc.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
        NetworkInterface actualValue =
                dc.getOption(StandardSocketOptions.IP_MULTICAST_IF);
        assertEquals(networkInterface, actualValue);
    }

    public void test_getOption_IP_MULTICAST_LOOP_defaults() throws Exception {
        DatagramChannel dc = DatagramChannel.open();

        assertTrue(dc.getOption(StandardSocketOptions.IP_MULTICAST_LOOP));

        dc.close();
    }

    public void test_getOption_IP_MULTICAST_LOOP_nullCheck() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        try {
            dc.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, null);
            fail();
        } catch (IllegalArgumentException expected) {}

        dc.close();
    }

    public void test_setOption_IP_MULTICAST_LOOP_afterOpen() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        assertTrue(dc.getOption(StandardSocketOptions.IP_MULTICAST_LOOP));

        dc.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false);
        assertFalse(dc.getOption(StandardSocketOptions.IP_MULTICAST_LOOP));

        dc.close();
    }

    public void test_setOption_IP_MULTICAST_LOOP_afterBind() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        dc.bind(new InetSocketAddress(Inet4Address.getLoopbackAddress(), 0));

        assertTrue(dc.getOption(StandardSocketOptions.IP_MULTICAST_LOOP));

        dc.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false);
        assertFalse(dc.getOption(StandardSocketOptions.IP_MULTICAST_LOOP));

        dc.close();
    }

    public void test_getOption_IP_MULTICAST_TTL_defaults() throws Exception {
        DatagramChannel dc = DatagramChannel.open();

        int value = dc.getOption(StandardSocketOptions.IP_MULTICAST_TTL);
        assertEquals(1, value);

        dc.close();
    }

    public void test_setOption_IP_MULTICAST_TTL_afterOpen() throws Exception {
        DatagramChannel dc = DatagramChannel.open();

        trySetMulticastTtlOption(dc);

        dc.close();
    }

    private static void trySetMulticastTtlOption(DatagramChannel dc) throws IOException {
        int initialValue = dc.getOption(StandardSocketOptions.IP_MULTICAST_TTL);
        try {
            dc.setOption(StandardSocketOptions.IP_MULTICAST_TTL, -1);
            fail();
        } catch (IllegalArgumentException expected) {}
        int actualValue = dc.getOption(StandardSocketOptions.IP_MULTICAST_TTL);
        assertEquals(initialValue, actualValue);

        int newTtl = initialValue + 1;
        dc.setOption(StandardSocketOptions.IP_MULTICAST_TTL, newTtl);
        actualValue = dc.getOption(StandardSocketOptions.IP_MULTICAST_TTL);
        assertEquals(newTtl, actualValue);
    }

    public void test_setOption_IP_MULTICAST_TTL_afterBind() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        dc.bind(null);

        trySetMulticastTtlOption(dc);

        dc.close();
    }

    public void test_getOption_SO_BROADCAST_defaults() throws Exception {
        DatagramChannel dc = DatagramChannel.open();

        assertFalse(dc.getOption(StandardSocketOptions.SO_BROADCAST));

        dc.close();
    }

    public void test_setOption_SO_BROADCAST_afterOpen() throws Exception {
        DatagramChannel dc = DatagramChannel.open();

        trySetSoBroadcastOption(dc);

        dc.close();
    }

    private static void trySetSoBroadcastOption(DatagramChannel dc) throws IOException {
        boolean initialValue = dc.getOption(StandardSocketOptions.SO_BROADCAST);

        dc.setOption(StandardSocketOptions.SO_BROADCAST, !initialValue);
        boolean actualValue = dc.getOption(StandardSocketOptions.SO_BROADCAST);
        assertEquals(!initialValue, actualValue);
    }

    public void test_setOption_SO_BROADCAST_afterBind() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        dc.bind(null);

        trySetSoBroadcastOption(dc);

        dc.close();
    }

    public void test_getOption_IP_TOS_defaults() throws Exception {
        DatagramChannel dc = DatagramChannel.open();

        int value = dc.getOption(StandardSocketOptions.IP_TOS);
        assertEquals(0, value);
        assertEquals(value, dc.socket().getTrafficClass());

        dc.close();
    }

    public void test_setOption_IP_TOS_afterOpen() throws Exception {
        DatagramChannel dc = DatagramChannel.open();

        trySetTosOption(dc);

        dc.close();
    }

    private static void trySetTosOption(DatagramChannel dc) throws IOException {
        int initialValue = dc.getOption(StandardSocketOptions.IP_TOS);
        try {
            dc.setOption(StandardSocketOptions.IP_TOS, -1);
            fail();
        } catch (IllegalArgumentException expected) {}
        assertEquals(initialValue, (int) dc.getOption(StandardSocketOptions.IP_TOS));
        assertEquals(initialValue, dc.socket().getTrafficClass());

        try {
            dc.setOption(StandardSocketOptions.IP_TOS, 256);
            fail();
        } catch (IllegalArgumentException expected) {}
        assertEquals(initialValue, (int) dc.getOption(StandardSocketOptions.IP_TOS));
        assertEquals(initialValue, dc.socket().getTrafficClass());

        int newValue = (initialValue + 1) % 255;
        dc.setOption(StandardSocketOptions.IP_TOS, newValue);
        assertEquals(newValue, (int) dc.getOption(StandardSocketOptions.IP_TOS));
        assertEquals(newValue, dc.socket().getTrafficClass());
    }

    public void test_setOption_IP_TOS_afterBind() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        dc.bind(null);

        trySetTosOption(dc);

        dc.close();
    }

    public void test_getOption_SO_REUSEADDR_defaults() throws Exception {
        DatagramChannel dc = DatagramChannel.open();

        boolean value = dc.getOption(StandardSocketOptions.SO_REUSEADDR);
        assertFalse(value);
        assertFalse(dc.socket().getReuseAddress());

        dc.close();
    }

    public void test_setOption_SO_REUSEADDR_afterOpen() throws Exception {
        DatagramChannel dc = DatagramChannel.open();

        boolean initialValue = dc.getOption(StandardSocketOptions.SO_REUSEADDR);
        dc.setOption(StandardSocketOptions.SO_REUSEADDR, !initialValue);
        assertEquals(!initialValue, (boolean) dc.getOption(StandardSocketOptions.SO_REUSEADDR));
        assertEquals(!initialValue, dc.socket().getReuseAddress());

        dc.close();
    }

}
