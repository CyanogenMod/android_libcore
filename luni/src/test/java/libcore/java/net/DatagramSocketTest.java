/*
 * Copyright (C) 2014 The Android Open Source Project
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

import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class DatagramSocketTest extends TestCase {

  public void testInitialState() throws Exception {
    DatagramSocket ds = new DatagramSocket();
    try {
      assertTrue(ds.isBound());
      assertTrue(ds.getBroadcast()); // The RI starts DatagramSocket in broadcast mode.
      assertFalse(ds.isClosed());
      assertFalse(ds.isConnected());
      assertTrue(ds.getLocalPort() > 0);
      assertTrue(ds.getLocalAddress().isAnyLocalAddress());
      InetSocketAddress socketAddress = (InetSocketAddress) ds.getLocalSocketAddress();
      assertEquals(ds.getLocalPort(), socketAddress.getPort());
      assertEquals(ds.getLocalAddress(), socketAddress.getAddress());
      assertNull(ds.getInetAddress());
      assertEquals(-1, ds.getPort());
      assertNull(ds.getRemoteSocketAddress());
      assertFalse(ds.getReuseAddress());
      assertNull(ds.getChannel());
    } finally {
      ds.close();
    }
  }

  public void testStateAfterClose() throws Exception {
    DatagramSocket ds = new DatagramSocket();
    ds.close();
    assertTrue(ds.isBound());
    assertTrue(ds.isClosed());
    assertFalse(ds.isConnected());
    assertNull(ds.getLocalAddress());
    assertEquals(-1, ds.getLocalPort());
    assertNull(ds.getLocalSocketAddress());
  }

  public void testPendingException() throws Exception {
    final int port = 9999;

    try (DatagramSocket s = new DatagramSocket()) {
      s.connect(InetAddress.getLocalHost(), port);

      // connect may set pendingConnectException on internal failure; since we have no reliable way
      // to make connect fail, set pendingConnectException through reflection.
      Field pendingConnectException = s.getClass().getDeclaredField("pendingConnectException");
      pendingConnectException.setAccessible(true);
      pendingConnectException.set(s, new SocketException());

      byte[] data = new byte[100];
      DatagramPacket p = new DatagramPacket(data, data.length);

      try {
        s.send(p);
        fail();
      } catch (SocketException expected) {
      }

      try {
        s.receive(p);
        fail();
      } catch (SocketException expected) {
      }
    }
  }

  // Socket should become connected even if impl.connect() failed and threw exception.
  public void test_b31218085() throws Exception {
    final int port = 9999;

    try (DatagramSocket s = new DatagramSocket()) {
      // Set fd of DatagramSocket to null, forcing impl.connect() to throw.
      Field f = DatagramSocket.class.getDeclaredField("impl");
      f.setAccessible(true);
      DatagramSocketImpl impl = (DatagramSocketImpl) f.get(s);
      f = DatagramSocketImpl.class.getDeclaredField("fd");
      f.setAccessible(true);
      f.set(impl, null);

      s.connect(InetAddress.getLocalHost(), port);
      assertTrue(s.isConnected());
    }
  }
}
