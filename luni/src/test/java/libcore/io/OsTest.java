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

package libcore.io;

import android.system.ErrnoException;
import android.system.NetlinkSocketAddress;
import android.system.OsConstants;
import android.system.PacketSocketAddress;
import android.system.StructTimeval;
import android.system.StructUcred;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InetUnixAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import junit.framework.TestCase;
import static android.system.OsConstants.*;

public class OsTest extends TestCase {
  public void testIsSocket() throws Exception {
    File f = new File("/dev/null");
    FileInputStream fis = new FileInputStream(f);
    assertFalse(S_ISSOCK(Libcore.os.fstat(fis.getFD()).st_mode));
    fis.close();

    ServerSocket s = new ServerSocket();
    assertTrue(S_ISSOCK(Libcore.os.fstat(s.getImpl$().getFD$()).st_mode));
    s.close();
  }

  public void testFcntlInt() throws Exception {
    File f = File.createTempFile("OsTest", "tst");
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(f);
      Libcore.os.fcntlInt(fis.getFD(), F_SETFD, FD_CLOEXEC);
      int flags = Libcore.os.fcntlVoid(fis.getFD(), F_GETFD);
      assertTrue((flags & FD_CLOEXEC) != 0);
    } finally {
      IoUtils.closeQuietly(fis);
      f.delete();
    }
  }

  public void testUnixDomainSockets_in_file_system() throws Exception {
    String path = System.getProperty("java.io.tmpdir") + "/test_unix_socket";
    new File(path).delete();
    checkUnixDomainSocket(new InetUnixAddress(path), false);
  }

  public void testUnixDomainSocket_abstract_name() throws Exception {
    // Linux treats a sun_path starting with a NUL byte as an abstract name. See unix(7).
    byte[] path = "/abstract_name_unix_socket".getBytes("UTF-8");
    path[0] = 0;
    checkUnixDomainSocket(new InetUnixAddress(path), true);
  }

  private void checkUnixDomainSocket(final InetUnixAddress address, final boolean isAbstract) throws Exception {
    final FileDescriptor serverFd = Libcore.os.socket(AF_UNIX, SOCK_STREAM, 0);
    Libcore.os.bind(serverFd, address, 0);
    Libcore.os.listen(serverFd, 5);

    checkSockName(serverFd, isAbstract, address);

    Thread server = new Thread(new Runnable() {
      public void run() {
        try {
          InetSocketAddress peerAddress = new InetSocketAddress();
          FileDescriptor clientFd = Libcore.os.accept(serverFd, peerAddress);
          checkSockName(clientFd, isAbstract, address);
          checkNoName(peerAddress);

          checkNoPeerName(clientFd);

          StructUcred credentials = Libcore.os.getsockoptUcred(clientFd, SOL_SOCKET, SO_PEERCRED);
          assertEquals(Libcore.os.getpid(), credentials.pid);
          assertEquals(Libcore.os.getuid(), credentials.uid);
          assertEquals(Libcore.os.getgid(), credentials.gid);

          byte[] request = new byte[256];
          Libcore.os.read(clientFd, request, 0, request.length);

          String s = new String(request, "UTF-8");
          byte[] response = s.toUpperCase(Locale.ROOT).getBytes("UTF-8");
          Libcore.os.write(clientFd, response, 0, response.length);

          Libcore.os.close(clientFd);
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    });
    server.start();

    FileDescriptor clientFd = Libcore.os.socket(AF_UNIX, SOCK_STREAM, 0);

    Libcore.os.connect(clientFd, address, 0);
    checkNoSockName(clientFd);

    String string = "hello, world!";

    byte[] request = string.getBytes("UTF-8");
    assertEquals(request.length, Libcore.os.write(clientFd, request, 0, request.length));

    byte[] response = new byte[request.length];
    assertEquals(response.length, Libcore.os.read(clientFd, response, 0, response.length));

    assertEquals(string.toUpperCase(Locale.ROOT), new String(response, "UTF-8"));

    Libcore.os.close(clientFd);
  }

  private void checkSockName(FileDescriptor fd, boolean isAbstract, InetAddress address) throws Exception {
    InetSocketAddress isa = (InetSocketAddress) Libcore.os.getsockname(fd);
    if (isAbstract) {
      checkNoName(isa);
    } else {
      assertEquals(address, isa.getAddress());
    }
  }

  private void checkNoName(SocketAddress sa) {
    InetSocketAddress isa = (InetSocketAddress) sa;
    assertEquals(0, isa.getAddress().getAddress().length);
  }

  private void checkNoPeerName(FileDescriptor fd) throws Exception {
    checkNoName(Libcore.os.getpeername(fd));
  }

  private void checkNoSockName(FileDescriptor fd) throws Exception {
    checkNoName(Libcore.os.getsockname(fd));
  }

  public void test_strsignal() throws Exception {
    assertEquals("Killed", Libcore.os.strsignal(9));
    assertEquals("Unknown signal -1", Libcore.os.strsignal(-1));
  }

  public void test_byteBufferPositions_write_pwrite() throws Exception {
    FileOutputStream fos = new FileOutputStream(new File("/dev/null"));
    FileDescriptor fd = fos.getFD();
    final byte[] contents = new String("goodbye, cruel world").getBytes(StandardCharsets.US_ASCII);
    ByteBuffer byteBuffer = ByteBuffer.wrap(contents);

    byteBuffer.position(0);
    int written = Libcore.os.write(fd, byteBuffer);
    assertTrue(written > 0);
    assertEquals(written, byteBuffer.position());

    byteBuffer.position(4);
    written = Libcore.os.write(fd, byteBuffer);
    assertTrue(written > 0);
    assertEquals(written + 4, byteBuffer.position());

    byteBuffer.position(0);
    written = Libcore.os.pwrite(fd, byteBuffer, 64 /* offset */);
    assertTrue(written > 0);
    assertEquals(written, byteBuffer.position());

    byteBuffer.position(4);
    written = Libcore.os.pwrite(fd, byteBuffer, 64 /* offset */);
    assertTrue(written > 0);
    assertEquals(written + 4, byteBuffer.position());

    fos.close();
  }

  public void test_byteBufferPositions_read_pread() throws Exception {
    FileInputStream fis = new FileInputStream(new File("/dev/zero"));
    FileDescriptor fd = fis.getFD();
    ByteBuffer byteBuffer = ByteBuffer.allocate(64);

    byteBuffer.position(0);
    int read = Libcore.os.read(fd, byteBuffer);
    assertTrue(read > 0);
    assertEquals(read, byteBuffer.position());

    byteBuffer.position(4);
    read = Libcore.os.read(fd, byteBuffer);
    assertTrue(read > 0);
    assertEquals(read + 4, byteBuffer.position());

    byteBuffer.position(0);
    read = Libcore.os.pread(fd, byteBuffer, 64 /* offset */);
    assertTrue(read > 0);
    assertEquals(read, byteBuffer.position());

    byteBuffer.position(4);
    read = Libcore.os.pread(fd, byteBuffer, 64 /* offset */);
    assertTrue(read > 0);
    assertEquals(read + 4, byteBuffer.position());

    fis.close();
  }

  static void checkByteBufferPositions_sendto_recvfrom(
          int family, InetAddress loopback) throws Exception {
    final FileDescriptor serverFd = Libcore.os.socket(family, SOCK_STREAM, 0);
    Libcore.os.bind(serverFd, loopback, 0);
    Libcore.os.listen(serverFd, 5);

    InetSocketAddress address = (InetSocketAddress) Libcore.os.getsockname(serverFd);

    final Thread server = new Thread(new Runnable() {
      public void run() {
        try {
          InetSocketAddress peerAddress = new InetSocketAddress();
          FileDescriptor clientFd = Libcore.os.accept(serverFd, peerAddress);

          // Attempt to receive a maximum of 24 bytes from the client, and then
          // close the connection.
          ByteBuffer buffer = ByteBuffer.allocate(16);
          int received = Libcore.os.recvfrom(clientFd, buffer, 0, null);
          assertTrue(received > 0);
          assertEquals(received, buffer.position());

          ByteBuffer buffer2 = ByteBuffer.allocate(16);
          buffer2.position(8);
          received = Libcore.os.recvfrom(clientFd, buffer2, 0, null);
          assertTrue(received > 0);
          assertEquals(received + 8, buffer.position());

          Libcore.os.close(clientFd);
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    });


    server.start();

    FileDescriptor clientFd = Libcore.os.socket(family, SOCK_STREAM, 0);
    Libcore.os.connect(clientFd, address.getAddress(), address.getPort());

    final byte[] bytes = "good bye, cruel black hole with fancy distortion".getBytes(StandardCharsets.US_ASCII);
    assertTrue(bytes.length > 24);

    ByteBuffer input = ByteBuffer.wrap(bytes);
    input.position(0);
    input.limit(16);

    int sent = Libcore.os.sendto(clientFd, input, 0, address.getAddress(), address.getPort());
    assertTrue(sent > 0);
    assertEquals(sent, input.position());

    input.position(16);
    input.limit(24);
    sent = Libcore.os.sendto(clientFd, input, 0, address.getAddress(), address.getPort());
    assertTrue(sent > 0);
    assertEquals(sent + 16, input.position());

    Libcore.os.close(clientFd);
  }

  public void test_NetlinkSocket() throws Exception {
    FileDescriptor nlSocket = Libcore.os.socket(AF_NETLINK, SOCK_DGRAM, NETLINK_ROUTE);
    Libcore.os.bind(nlSocket, new NetlinkSocketAddress());
    NetlinkSocketAddress address = (NetlinkSocketAddress) Libcore.os.getsockname(nlSocket);
    assertTrue(address.getPortId() > 0);
    assertEquals(0, address.getGroupsMask());

    NetlinkSocketAddress nlKernel = new NetlinkSocketAddress();
    Libcore.os.connect(nlSocket, nlKernel);
    NetlinkSocketAddress nlPeer = (NetlinkSocketAddress) Libcore.os.getpeername(nlSocket);
    assertEquals(0, nlPeer.getPortId());
    assertEquals(0, nlPeer.getGroupsMask());
    Libcore.os.close(nlSocket);
  }

  public void test_PacketSocketAddress() throws Exception {
    NetworkInterface lo = NetworkInterface.getByName("lo");
    FileDescriptor fd = Libcore.os.socket(AF_PACKET, SOCK_DGRAM, ETH_P_IPV6);
    PacketSocketAddress addr = new PacketSocketAddress((short) ETH_P_IPV6, lo.getIndex());
    Libcore.os.bind(fd, addr);

    PacketSocketAddress bound = (PacketSocketAddress) Libcore.os.getsockname(fd);
    assertEquals((short) ETH_P_IPV6, bound.sll_protocol);  // ETH_P_IPV6 is an int.
    assertEquals(lo.getIndex(), bound.sll_ifindex);
    assertEquals(ARPHRD_LOOPBACK, bound.sll_hatype);
    assertEquals(0, bound.sll_pkttype);

    // The loopback address is ETH_ALEN bytes long and is all zeros.
    // http://lxr.free-electrons.com/source/drivers/net/loopback.c?v=3.10#L167
    assertEquals(6, bound.sll_addr.length);
    for (int i = 0; i < 6; i++) {
      assertEquals(0, bound.sll_addr[i]);
    }
  }

  public void test_byteBufferPositions_sendto_recvfrom_af_inet() throws Exception {
    checkByteBufferPositions_sendto_recvfrom(AF_INET, InetAddress.getByName("127.0.0.1"));
  }

  public void test_byteBufferPositions_sendto_recvfrom_af_inet6() throws Exception {
    checkByteBufferPositions_sendto_recvfrom(AF_INET6, InetAddress.getByName("::1"));
  }

  private void checkSendToSocketAddress(int family, InetAddress loopback) throws Exception {
    FileDescriptor recvFd = Libcore.os.socket(family, SOCK_DGRAM, 0);
    Libcore.os.bind(recvFd, loopback, 0);
    StructTimeval tv = StructTimeval.fromMillis(20);
    Libcore.os.setsockoptTimeval(recvFd, SOL_SOCKET, SO_RCVTIMEO, tv);

    InetSocketAddress to = ((InetSocketAddress) Libcore.os.getsockname(recvFd));
    FileDescriptor sendFd = Libcore.os.socket(family, SOCK_DGRAM, 0);
    byte[] msg = ("Hello, I'm going to a socket address: " + to.toString()).getBytes("UTF-8");
    int len = msg.length;

    assertEquals(len, Libcore.os.sendto(sendFd, msg, 0, len, 0, to));
    byte[] received = new byte[msg.length + 42];
    InetSocketAddress from = new InetSocketAddress();
    assertEquals(len, Libcore.os.recvfrom(recvFd, received, 0, received.length, 0, from));
    assertEquals(loopback, from.getAddress());
  }

  public void test_sendtoSocketAddress_af_inet() throws Exception {
      checkSendToSocketAddress(AF_INET, InetAddress.getByName("127.0.0.1"));
  }

  public void test_sendtoSocketAddress_af_inet6() throws Exception {
      checkSendToSocketAddress(AF_INET6, InetAddress.getByName("::1"));
  }

  public void test_socketFamilies() throws Exception {
    FileDescriptor fd = Libcore.os.socket(AF_INET6, SOCK_STREAM, 0);
    Libcore.os.bind(fd, InetAddress.getByName("::"), 0);
    InetSocketAddress localSocketAddress = (InetSocketAddress) Libcore.os.getsockname(fd);
    assertEquals(Inet6Address.ANY, localSocketAddress.getAddress());

    fd = Libcore.os.socket(AF_INET6, SOCK_STREAM, 0);
    Libcore.os.bind(fd, InetAddress.getByName("0.0.0.0"), 0);
    localSocketAddress = (InetSocketAddress) Libcore.os.getsockname(fd);
    assertEquals(Inet6Address.ANY, localSocketAddress.getAddress());

    fd = Libcore.os.socket(AF_INET, SOCK_STREAM, 0);
    Libcore.os.bind(fd, InetAddress.getByName("0.0.0.0"), 0);
    localSocketAddress = (InetSocketAddress) Libcore.os.getsockname(fd);
    assertEquals(Inet4Address.ANY, localSocketAddress.getAddress());
    try {
      Libcore.os.bind(fd, InetAddress.getByName("::"), 0);
      fail("Expected ErrnoException binding IPv4 socket to ::");
    } catch (ErrnoException expected) {
      assertEquals("Expected EAFNOSUPPORT binding IPv4 socket to ::", EAFNOSUPPORT, expected.errno);
    }
  }

  private static void assertArrayEquals(byte[] expected, byte[] actual) {
    assertTrue("Expected=" + Arrays.toString(expected) + ", actual=" + Arrays.toString(actual),
               Arrays.equals(expected, actual));
  }

  private static void checkSocketPing(FileDescriptor fd, InetAddress to, byte[] packet,
          byte type, byte responseType, boolean useSendto) throws Exception {
    int len = packet.length;
    packet[0] = type;
    if (useSendto) {
      assertEquals(len, Libcore.os.sendto(fd, packet, 0, len, 0, to, 0));
    } else {
      Libcore.os.connect(fd, to, 0);
      assertEquals(len, Libcore.os.sendto(fd, packet, 0, len, 0, null, 0));
    }

    int icmpId = ((InetSocketAddress) Libcore.os.getsockname(fd)).getPort();
    byte[] received = new byte[4096];
    InetSocketAddress srcAddress = new InetSocketAddress();
    assertEquals(len, Libcore.os.recvfrom(fd, received, 0, received.length, 0, srcAddress));
    assertEquals(to, srcAddress.getAddress());
    assertEquals(responseType, received[0]);
    assertEquals(received[4], (byte) (icmpId >> 8));
    assertEquals(received[5], (byte) (icmpId & 0xff));

    received = Arrays.copyOf(received, len);
    received[0] = (byte) type;
    received[2] = received[3] = 0;  // Checksum.
    received[4] = received[5] = 0;  // ICMP ID.
    assertArrayEquals(packet, received);
  }

  public void test_socketPing() throws Exception {
    final byte ICMP_ECHO = 8, ICMP_ECHOREPLY = 0;
    final byte ICMPV6_ECHO_REQUEST = (byte) 128, ICMPV6_ECHO_REPLY = (byte) 129;
    final byte[] packet = ("\000\000\000\000" +  // ICMP type, code.
                           "\000\000\000\003" +  // ICMP ID (== port), sequence number.
                           "Hello myself").getBytes(StandardCharsets.US_ASCII);

    FileDescriptor fd = Libcore.os.socket(AF_INET6, SOCK_DGRAM, IPPROTO_ICMPV6);
    InetAddress ipv6Loopback = InetAddress.getByName("::1");
    checkSocketPing(fd, ipv6Loopback, packet, ICMPV6_ECHO_REQUEST, ICMPV6_ECHO_REPLY, true);
    checkSocketPing(fd, ipv6Loopback, packet, ICMPV6_ECHO_REQUEST, ICMPV6_ECHO_REPLY, false);

    fd = Libcore.os.socket(AF_INET, SOCK_DGRAM, IPPROTO_ICMP);
    InetAddress ipv4Loopback = InetAddress.getByName("127.0.0.1");
    checkSocketPing(fd, ipv4Loopback, packet, ICMP_ECHO, ICMP_ECHOREPLY, true);
    checkSocketPing(fd, ipv4Loopback, packet, ICMP_ECHO, ICMP_ECHOREPLY, false);
  }

    private static void assertPartial(byte[] expected, byte[] actual) {
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != actual[i]) {
                fail("Expected " + Arrays.toString(expected) + " but found "
                        + Arrays.toString(actual));
            }
        }
    }

    public void test_xattr() throws Exception {
        final String NAME_TEST = "user.meow";

        final byte[] VALUE_CAKE = "cake cake cake".getBytes(StandardCharsets.UTF_8);
        final byte[] VALUE_PIE = "pie".getBytes(StandardCharsets.UTF_8);

        File file = File.createTempFile("xattr", "test");
        String path = file.getAbsolutePath();

        byte[] tmp = new byte[1024];
        try {
            try {
                Libcore.os.getxattr(path, NAME_TEST, tmp);
                fail("Expected ENODATA");
            } catch (ErrnoException e) {
                assertEquals(OsConstants.ENODATA, e.errno);
            }

            Libcore.os.setxattr(path, NAME_TEST, VALUE_CAKE, OsConstants.XATTR_CREATE);
            assertEquals(VALUE_CAKE.length, Libcore.os.getxattr(path, NAME_TEST, tmp));
            assertPartial(VALUE_CAKE, tmp);

            try {
                Libcore.os.setxattr(path, NAME_TEST, VALUE_PIE, OsConstants.XATTR_CREATE);
                fail("Expected EEXIST");
            } catch (ErrnoException e) {
                assertEquals(OsConstants.EEXIST, e.errno);
            }

            Libcore.os.setxattr(path, NAME_TEST, VALUE_PIE, OsConstants.XATTR_REPLACE);
            assertEquals(VALUE_PIE.length, Libcore.os.getxattr(path, NAME_TEST, tmp));
            assertPartial(VALUE_PIE, tmp);

            Libcore.os.removexattr(path, NAME_TEST);
            try {
                Libcore.os.getxattr(path, NAME_TEST, tmp);
                fail("Expected ENODATA");
            } catch (ErrnoException e) {
                assertEquals(OsConstants.ENODATA, e.errno);
            }

        } finally {
            file.delete();
        }
    }
}
