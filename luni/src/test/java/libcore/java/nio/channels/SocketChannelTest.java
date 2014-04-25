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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.UnresolvedAddressException;
import java.util.Set;

import tests.io.MockOs;

import static android.system.OsConstants.*;

public class SocketChannelTest extends junit.framework.TestCase {

  private final MockOs mockOs = new MockOs();

  @Override
  public void setUp() throws Exception {
    mockOs.install();
  }

  @Override
  protected void tearDown() throws Exception {
    mockOs.uninstall();
  }

  public void test_read_intoReadOnlyByteArrays() throws Exception {
    ByteBuffer readOnly = ByteBuffer.allocate(1).asReadOnlyBuffer();
    ServerSocket ss = new ServerSocket(0);
    ss.setReuseAddress(true);
    SocketChannel sc = SocketChannel.open(ss.getLocalSocketAddress());
    try {
      sc.read(readOnly);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      sc.read(new ByteBuffer[] { readOnly });
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      sc.read(new ByteBuffer[] { readOnly }, 0, 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  // https://code.google.com/p/android/issues/detail?id=56684
  public void test_56684() throws Exception {
    mockOs.enqueueFault("connect", ENETUNREACH);

    SocketChannel sc = SocketChannel.open();
    sc.configureBlocking(false);

    Selector selector = Selector.open();
    SelectionKey selectionKey = sc.register(selector, SelectionKey.OP_CONNECT);

    try {
      sc.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 }), 0));
      fail();
    } catch (ConnectException ex) {
    }

    try {
      sc.finishConnect();
      fail();
    } catch (ClosedChannelException expected) {
    }
  }

  /** Checks that closing a Socket's output stream also closes the Socket and SocketChannel. */
  public void test_channelSocketOutputStreamClosureState() throws Exception {
    ServerSocket ss = new ServerSocket(0);

    SocketChannel sc = SocketChannel.open(ss.getLocalSocketAddress());
    sc.configureBlocking(true);

    Socket scSocket = sc.socket();
    OutputStream os = scSocket.getOutputStream();

    assertTrue(sc.isOpen());
    assertFalse(scSocket.isClosed());

    os.close();

    assertFalse(sc.isOpen());
    assertTrue(scSocket.isClosed());

    ss.close();
  }

  /** Checks that closing a Socket's input stream also closes the Socket and SocketChannel. */
  public void test_channelSocketInputStreamClosureState() throws Exception {
    ServerSocket ss = new ServerSocket(0);

    SocketChannel sc = SocketChannel.open(ss.getLocalSocketAddress());
    sc.configureBlocking(true);

    Socket scSocket = sc.socket();
    InputStream is = scSocket.getInputStream();

    assertTrue(sc.isOpen());
    assertFalse(scSocket.isClosed());

    is.close();

    assertFalse(sc.isOpen());
    assertTrue(scSocket.isClosed());

    ss.close();
  }

  /** Checks the state of the SocketChannel and associated Socket after open() */
  public void test_open_initialState() throws Exception {
    SocketChannel sc = SocketChannel.open();
    try {
      assertNull(sc.getLocalAddress());

      Socket socket = sc.socket();
      assertFalse(socket.isBound());
      assertFalse(socket.isClosed());
      assertFalse(socket.isConnected());
      assertEquals(-1, socket.getLocalPort());
      assertTrue(socket.getLocalAddress().isAnyLocalAddress());
      assertNull(socket.getLocalSocketAddress());
      assertNull(socket.getInetAddress());
      assertEquals(0, socket.getPort());
      assertNull(socket.getRemoteSocketAddress());
      assertFalse(socket.getReuseAddress());

      assertSame(sc, socket.getChannel());
    } finally {
      sc.close();
    }
  }

  public void test_bind_unresolvedAddress() throws IOException {
    SocketChannel sc = SocketChannel.open();
    try {
      sc.bind(new InetSocketAddress("unresolvedname", 31415));
      fail();
    } catch (UnresolvedAddressException expected) {
    }

    assertNull(sc.getLocalAddress());
    assertTrue(sc.isOpen());
    assertFalse(sc.isConnected());

    sc.close();
  }

  /** Checks that the SocketChannel and associated Socket agree on the socket state. */
  public void test_bind_socketStateSync() throws IOException {
    SocketChannel sc = SocketChannel.open();
    assertNull(sc.getLocalAddress());

    Socket socket = sc.socket();
    assertNull(socket.getLocalSocketAddress());
    assertFalse(socket.isBound());

    InetSocketAddress bindAddr = new InetSocketAddress("localhost", 0);
    sc.bind(bindAddr);

    InetSocketAddress actualAddr = (InetSocketAddress) sc.getLocalAddress();
    assertEquals(actualAddr, socket.getLocalSocketAddress());
    assertEquals(bindAddr.getHostName(), actualAddr.getHostName());
    assertTrue(socket.isBound());
    assertFalse(socket.isConnected());
    assertFalse(socket.isClosed());

    sc.close();

    assertFalse(sc.isOpen());
    assertTrue(socket.isClosed());
  }

  /**
   * Checks that the SocketChannel and associated Socket agree on the socket state, even if
   * the Socket object is requested/created after bind().
   */
  public void test_bind_socketObjectCreationAfterBind() throws IOException {
    SocketChannel sc = SocketChannel.open();
    assertNull(sc.getLocalAddress());

    InetSocketAddress bindAddr = new InetSocketAddress("localhost", 0);
    sc.bind(bindAddr);

    // Socket object creation after bind().
    Socket socket = sc.socket();
    InetSocketAddress actualAddr = (InetSocketAddress) sc.getLocalAddress();
    assertEquals(actualAddr, socket.getLocalSocketAddress());
    assertEquals(bindAddr.getHostName(), actualAddr.getHostName());
    assertTrue(socket.isBound());
    assertFalse(socket.isConnected());
    assertFalse(socket.isClosed());

    sc.close();

    assertFalse(sc.isOpen());
    assertTrue(socket.isClosed());
  }

  /**
   * Tests connect() and object state for a blocking SocketChannel. Blocking mode is the default.
   */
  public void test_connect_blocking() throws Exception {
    ServerSocket ss = new ServerSocket(0);

    SocketChannel sc = SocketChannel.open();
    assertTrue(sc.isBlocking());

    assertTrue(sc.connect(ss.getLocalSocketAddress()));

    assertTrue(sc.socket().isBound());
    assertTrue(sc.isConnected());
    assertTrue(sc.socket().isConnected());
    assertFalse(sc.socket().isClosed());
    assertTrue(sc.isBlocking());

    ss.close();
    sc.close();
  }

  /** Tests connect() and object state for a non-blocking SocketChannel. */
  public void test_connect_nonBlocking() throws Exception {
    ServerSocket ss = new ServerSocket(0);

    SocketChannel sc = SocketChannel.open();
    assertTrue(sc.isBlocking());
    sc.configureBlocking(false);
    assertFalse(sc.isBlocking());

    if (!sc.connect(ss.getLocalSocketAddress())) {
      do {
        assertTrue(sc.socket().isBound());
        assertFalse(sc.isConnected());
        assertFalse(sc.socket().isConnected());
        assertFalse(sc.socket().isClosed());
      } while (!sc.finishConnect());
    }
    assertTrue(sc.socket().isBound());
    assertTrue(sc.isConnected());
    assertTrue(sc.socket().isConnected());
    assertFalse(sc.socket().isClosed());
    assertFalse(sc.isBlocking());

    ss.close();
    sc.close();
  }

  public void test_supportedOptions() throws Exception {
    SocketChannel sc = SocketChannel.open();
    Set<SocketOption<?>> options = sc.supportedOptions();

    // Probe some values. This is not intended to be complete.
    assertTrue(options.contains(StandardSocketOptions.SO_REUSEADDR));
    assertFalse(options.contains(StandardSocketOptions.IP_MULTICAST_TTL));
  }

  public void test_getOption_unsupportedOption() throws Exception {
    SocketChannel sc = SocketChannel.open();
    try {
      sc.getOption(StandardSocketOptions.IP_MULTICAST_TTL);
      fail();
    } catch (UnsupportedOperationException expected) {
    }

    sc.close();
  }

  public void test_getOption_afterClose() throws Exception {
    SocketChannel sc = SocketChannel.open();
    sc.close();

    try {
      sc.getOption(StandardSocketOptions.SO_RCVBUF);
      fail();
    } catch (ClosedChannelException expected) {
    }
  }

  public void test_setOption_afterClose() throws Exception {
    SocketChannel sc = SocketChannel.open();
    sc.close();

    try {
      sc.setOption(StandardSocketOptions.SO_RCVBUF, 1234);
      fail();
    } catch (ClosedChannelException expected) {
    }
  }

  public void test_getOption_SO_RCVBUF_defaults() throws Exception {
    SocketChannel sc = SocketChannel.open();

    int value = sc.getOption(StandardSocketOptions.SO_RCVBUF);
    assertTrue(value > 0);
    assertEquals(value, sc.socket().getReceiveBufferSize());

    sc.close();
  }

  public void test_setOption_SO_RCVBUF_afterOpen() throws Exception {
    SocketChannel sc = SocketChannel.open();

    trySetReceiveBufferSizeOption(sc);

    sc.close();
  }

  private static void trySetReceiveBufferSizeOption(SocketChannel sc) throws IOException {
    int initialValue = sc.getOption(StandardSocketOptions.SO_RCVBUF);
    try {
      sc.setOption(StandardSocketOptions.SO_RCVBUF, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    int actualValue = sc.getOption(StandardSocketOptions.SO_RCVBUF);
    assertEquals(initialValue, actualValue);
    assertEquals(initialValue, sc.socket().getReceiveBufferSize());

    int newBufferSize = initialValue - 1;
    sc.setOption(StandardSocketOptions.SO_RCVBUF, newBufferSize);
    actualValue = sc.getOption(StandardSocketOptions.SO_RCVBUF);
    // The Linux Kernel actually doubles the value it is given and may choose to ignore it.
    // This assertion may be brittle.
    assertTrue(actualValue != initialValue);
    assertEquals(actualValue, sc.socket().getReceiveBufferSize());
  }

  public void test_getOption_SO_SNDBUF_defaults() throws Exception {
    SocketChannel sc = SocketChannel.open();

    int bufferSize = sc.getOption(StandardSocketOptions.SO_SNDBUF);
    assertTrue(bufferSize > 0);
    assertEquals(bufferSize, sc.socket().getSendBufferSize());

    sc.close();
  }

  public void test_setOption_SO_SNDBUF_afterOpen() throws Exception {
    SocketChannel sc = SocketChannel.open();

    trySetSendBufferSizeOption(sc);

    sc.close();
  }

  private static void trySetSendBufferSizeOption(SocketChannel sc) throws IOException {
    int initialValue = sc.getOption(StandardSocketOptions.SO_SNDBUF);
    try {
      sc.setOption(StandardSocketOptions.SO_SNDBUF, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    int actualValue = sc.getOption(StandardSocketOptions.SO_SNDBUF);
    assertEquals(initialValue, actualValue);
    assertEquals(initialValue, sc.socket().getSendBufferSize());

    int newValue = initialValue - 1;
    sc.setOption(StandardSocketOptions.SO_SNDBUF, newValue);
    actualValue = sc.getOption(StandardSocketOptions.SO_SNDBUF);
    // The Linux Kernel actually doubles the value it is given and may choose to ignore it.
    // This assertion may be brittle.
    assertTrue(actualValue != initialValue);
    assertEquals(actualValue, sc.socket().getSendBufferSize());
  }

  public void test_getOption_SO_KEEPALIVE_defaults() throws Exception {
    SocketChannel sc = SocketChannel.open();

    assertFalse(sc.getOption(StandardSocketOptions.SO_KEEPALIVE));

    sc.close();
  }

  public void test_setOption_SO_KEEPALIVE_afterOpen() throws Exception {
    SocketChannel sc = SocketChannel.open();

    trySetSoKeepaliveOption(sc);

    sc.close();
  }

  private static void trySetSoKeepaliveOption(SocketChannel sc) throws IOException {
    boolean initialValue = sc.getOption(StandardSocketOptions.SO_KEEPALIVE);

    sc.setOption(StandardSocketOptions.SO_KEEPALIVE, !initialValue);
    boolean actualValue = sc.getOption(StandardSocketOptions.SO_KEEPALIVE);
    assertEquals(!initialValue, actualValue);
  }

  public void test_setOption_SO_KEEPALIVE_afterBind() throws Exception {
    SocketChannel sc = SocketChannel.open();
    sc.bind(null);

    trySetSoKeepaliveOption(sc);

    sc.close();
  }

  public void test_getOption_IP_TOS_defaults() throws Exception {
    SocketChannel sc = SocketChannel.open();

    int value = sc.getOption(StandardSocketOptions.IP_TOS);
    assertEquals(0, value);
    assertEquals(value, sc.socket().getTrafficClass());

    sc.close();
  }

  public void test_setOption_IP_TOS_afterOpen() throws Exception {
    SocketChannel sc = SocketChannel.open();

    trySetTosOption(sc);

    sc.close();
  }

  private static void trySetTosOption(SocketChannel sc) throws IOException {
    int initialValue = sc.getOption(StandardSocketOptions.IP_TOS);
    try {
      sc.setOption(StandardSocketOptions.IP_TOS, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    assertEquals(initialValue, (int) sc.getOption(StandardSocketOptions.IP_TOS));
    assertEquals(initialValue, sc.socket().getTrafficClass());

    try {
      sc.setOption(StandardSocketOptions.IP_TOS, 256);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    assertEquals(initialValue, (int) sc.getOption(StandardSocketOptions.IP_TOS));
    assertEquals(initialValue, sc.socket().getTrafficClass());

    int newValue = (initialValue + 1) % 255;
    sc.setOption(StandardSocketOptions.IP_TOS, newValue);
    assertEquals(newValue, (int) sc.getOption(StandardSocketOptions.IP_TOS));
    assertEquals(newValue, sc.socket().getTrafficClass());
  }

  public void test_setOption_IP_TOS_afterBind() throws Exception {
    SocketChannel sc = SocketChannel.open();
    sc.bind(null);

    trySetTosOption(sc);

    sc.close();
  }

  public void test_getOption_SO_LINGER_defaults() throws Exception {
    SocketChannel sc = SocketChannel.open();

    int value = sc.getOption(StandardSocketOptions.SO_LINGER);
    assertTrue(value < 0);
    assertEquals(value, sc.socket().getSoLinger());

    sc.close();
  }

  public void test_setOption_SO_LINGER_afterOpen() throws Exception {
    SocketChannel sc = SocketChannel.open();

    trySetLingerOption(sc);

    sc.close();
  }

  private static void trySetLingerOption(SocketChannel sc) throws IOException {
    int initialValue = sc.getOption(StandardSocketOptions.SO_LINGER);
    // Any negative value disables the setting, -1 is used to report SO_LINGER being disabled.
    sc.setOption(StandardSocketOptions.SO_LINGER, -2);
    int soLingerDisabled = -1;
    assertEquals(soLingerDisabled, (int) sc.getOption(StandardSocketOptions.SO_LINGER));
    assertEquals(soLingerDisabled, sc.socket().getSoLinger());

    sc.setOption(StandardSocketOptions.SO_LINGER, 65536);
    assertEquals(65535, (int) sc.getOption(StandardSocketOptions.SO_LINGER));
    assertEquals(65535, sc.socket().getSoLinger());

    int newValue = (initialValue + 1) % 65535;
    sc.setOption(StandardSocketOptions.SO_LINGER, newValue);
    assertEquals(newValue, (int) sc.getOption(StandardSocketOptions.SO_LINGER));
    assertEquals(newValue, sc.socket().getSoLinger());
  }

  public void test_setOption_SO_LINGER_afterBind() throws Exception {
    SocketChannel sc = SocketChannel.open();
    sc.bind(null);

    trySetLingerOption(sc);

    sc.close();
  }

  public void test_getOption_SO_REUSEADDR_defaults() throws Exception {
    SocketChannel sc = SocketChannel.open();

    boolean value = sc.getOption(StandardSocketOptions.SO_REUSEADDR);
    assertFalse(value);
    assertFalse(sc.socket().getReuseAddress());

    sc.close();
  }

  public void test_setOption_SO_REUSEADDR_afterOpen() throws Exception {
    SocketChannel sc = SocketChannel.open();

    boolean initialValue = sc.getOption(StandardSocketOptions.SO_REUSEADDR);
    sc.setOption(StandardSocketOptions.SO_REUSEADDR, !initialValue);
    assertEquals(!initialValue, (boolean) sc.getOption(StandardSocketOptions.SO_REUSEADDR));
    assertEquals(!initialValue, sc.socket().getReuseAddress());

    sc.close();
  }

  public void test_getOption_TCP_NODELAY_defaults() throws Exception {
    SocketChannel sc = SocketChannel.open();

    boolean value = sc.getOption(StandardSocketOptions.TCP_NODELAY);
    assertFalse(value);
    assertFalse(sc.socket().getTcpNoDelay());

    sc.close();
  }

  public void test_setOption_TCP_NODELAY_afterOpen() throws Exception {
    SocketChannel sc = SocketChannel.open();

    trySetNoDelay(sc);

    sc.close();
  }

  private static void trySetNoDelay(SocketChannel sc) throws IOException {
    boolean initialValue = sc.getOption(StandardSocketOptions.TCP_NODELAY);
    sc.setOption(StandardSocketOptions.TCP_NODELAY, !initialValue);
    assertEquals(!initialValue, (boolean) sc.getOption(StandardSocketOptions.TCP_NODELAY));
    assertEquals(!initialValue, sc.socket().getTcpNoDelay());
  }

  public void test_setOption_TCP_NODELAY_afterBind() throws Exception {
    SocketChannel sc = SocketChannel.open();
    sc.bind(null);

    trySetNoDelay(sc);

    sc.close();
  }

}
