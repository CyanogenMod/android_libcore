/*
 * Copyright (C) 2009 The Android Open Source Project
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

package java.net;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class SocketTest extends junit.framework.TestCase {
    /**
     * Our getLocalAddress and getLocalPort currently use getsockname(3).
     * This means they give incorrect results on closed sockets (as well
     * as requiring an unnecessary call into native code).
     */
    public void test_getLocalAddress_after_close() throws Exception {
        Socket s = new Socket();
        try {
            // Bind to an ephemeral local port.
            s.bind(new InetSocketAddress("localhost", 0));
            assertTrue(s.getLocalAddress().isLoopbackAddress());
            // What local port did we get?
            int localPort = s.getLocalPort();
            assertTrue(localPort > 0);
            // Now close the socket...
            s.close();
            // The RI returns the ANY address but the original local port after close.
            assertTrue(s.getLocalAddress().isAnyLocalAddress());
            assertEquals(localPort, s.getLocalPort());
        } finally {
            s.close();
        }
    }

    // http://code.google.com/p/android/issues/detail?id=7935
    public void test_newSocket_connection_refused() throws Exception {
        try {
            new Socket("localhost", 80);
            fail("connection should have been refused");
        } catch (ConnectException expected) {
        }
    }

    // http://code.google.com/p/android/issues/detail?id=3123
    // http://code.google.com/p/android/issues/detail?id=1933
    public void test_socketLocalAndRemoteAddresses() throws Exception {
        checkSocketLocalAndRemoteAddresses(false);
        checkSocketLocalAndRemoteAddresses(true);
    }

    public void checkSocketLocalAndRemoteAddresses(boolean setOptions) throws Exception {
        InetAddress host = InetAddress.getLocalHost();

        // Open a local server port.
        ServerSocketChannel ssc = ServerSocketChannel.open();
        InetSocketAddress listenAddr = new InetSocketAddress(host, 0);
        ssc.socket().bind(listenAddr, 0);
        ServerSocket ss = ssc.socket();

        // Open a socket to the local port.
        SocketChannel out = SocketChannel.open();
        out.configureBlocking(false);
        if (setOptions) {
            out.socket().setTcpNoDelay(false);
        }
        InetSocketAddress addr = new InetSocketAddress(host, ssc.socket().getLocalPort());
        out.connect(addr);
        while (!out.finishConnect()) {
            Thread.sleep(1);
        }

        SocketChannel in = ssc.accept();
        if (setOptions) {
            in.socket().setTcpNoDelay(false);
        }

        InetSocketAddress outRemoteAddress = (InetSocketAddress) out.socket().getRemoteSocketAddress();
        InetSocketAddress outLocalAddress = (InetSocketAddress) out.socket().getLocalSocketAddress();
        InetSocketAddress inLocalAddress = (InetSocketAddress) in.socket().getLocalSocketAddress();
        InetSocketAddress inRemoteAddress = (InetSocketAddress) in.socket().getRemoteSocketAddress();
        System.err.println("inLocalAddress: " + inLocalAddress);
        System.err.println("inRemoteAddress: " + inRemoteAddress);
        System.err.println("outLocalAddress: " + outLocalAddress);
        System.err.println("outRemoteAddress: " + outRemoteAddress);

        assertEquals(outRemoteAddress.getPort(), ss.getLocalPort());
        assertEquals(inLocalAddress.getPort(), ss.getLocalPort());
        assertEquals(inRemoteAddress.getPort(), outLocalAddress.getPort());

        assertEquals(inLocalAddress.getAddress(), ss.getInetAddress());
        assertEquals(inRemoteAddress.getAddress(), ss.getInetAddress());
        assertEquals(outLocalAddress.getAddress(), ss.getInetAddress());
        assertEquals(outRemoteAddress.getAddress(), ss.getInetAddress());

        in.close();
        out.close();
        ssc.close();

        assertNull(in.socket().getRemoteSocketAddress());
        assertNull(out.socket().getRemoteSocketAddress());

        assertEquals(in.socket().getLocalSocketAddress(), ss.getLocalSocketAddress());
    }
}
