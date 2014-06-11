package libcore.java.nio.channels;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Tests associated with multicast behavior of DatagramChannel.
 */
public class DatagramChannelMulticastTest extends TestCase {

  private static InetAddress lookup(String s) {
    try {
      return InetAddress.getByName(s);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  // These IP addresses aren't inherently "good" or "bad"; they're just used like that.
  // We use the "good" addresses for our actual group, and the "bad" addresses are for a group that
  // we won't actually set up.
  private static final InetAddress GOOD_MULTICAST_IPv4 = lookup("239.255.0.1");
  private static final InetAddress BAD_MULTICAST_IPv4 = lookup("239.255.0.2");
  private static final InetAddress GOOD_MULTICAST_IPv6 = lookup("ff05::7:7");
  private static final InetAddress BAD_MULTICAST_IPv6 = lookup("ff05::7:8");

  // Special addresses.
  private static final InetAddress WILDCARD_IPv4 = lookup("0.0.0.0");
  private static final InetAddress WILDCARD_IPv6 = lookup("::");

  // Arbitrary unicast addresses. Used when the value doesn't actually matter. e.g. for source
  // filters.
  private static final InetAddress UNICAST_IPv4_1 = lookup("192.168.1.1");
  private static final InetAddress UNICAST_IPv4_2 = lookup("192.168.1.2");
  private static final InetAddress UNICAST_IPv6_1 = lookup("2001:db8::1");
  private static final InetAddress UNICAST_IPv6_2 = lookup("2001:db8::2");

  private List<NetworkInterface> ipv4networkInterfaces = new ArrayList<NetworkInterface>();
  private List<NetworkInterface> ipv6networkInterfaces = new ArrayList<NetworkInterface>();
  private NetworkInterface ipv4networkInterface;
  private NetworkInterface ipv6networkInterface;
  private NetworkInterface loopbackInterface;

  @Override
  protected void setUp() throws Exception {
    // The loopback interface isn't actually useful for sending/receiving multicast messages but it
    // can be used as a dummy for tests where that does not matter.
    loopbackInterface = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
    assertNotNull(loopbackInterface);
    assertTrue(loopbackInterface.isLoopback());
    assertFalse(loopbackInterface.supportsMulticast());

    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    assertNotNull(interfaces);
    // Only consider interfaces that have addresses associated with them. Otherwise tests don't work
    // so well.
    while (interfaces.hasMoreElements()) {
      NetworkInterface networkInterface = interfaces.nextElement();
      if (willWorkForMulticast(networkInterface)) {
        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
          final InetAddress nextAddress = addresses.nextElement();
          if (nextAddress instanceof Inet4Address) {
            ipv4networkInterfaces.add(networkInterface);
          } else if (nextAddress instanceof Inet6Address) {
            ipv6networkInterfaces.add(networkInterface);
          }
        }
      }
    }
    assertTrue(
        "Test environment must have network interfaces capable of both IPv4 and IPv6 multicast",
        ipv4networkInterfaces.size() > 0 && ipv6networkInterfaces.size() > 0);
    ipv4networkInterface = ipv4networkInterfaces.get(0);
    ipv6networkInterface = ipv6networkInterfaces.get(0);
  }

  public void test_open() throws IOException {
    DatagramChannel dc = DatagramChannel.open();

    // Unlike MulticastSocket, DatagramChannel has SO_REUSEADDR set to false by default.
    assertFalse(dc.getOption(StandardSocketOptions.SO_REUSEADDR));

    // Confirm multicast loop is on by default as specified in the docs. Many tests in this class
    // depend on this being true.
    assertTrue(dc.getOption(StandardSocketOptions.IP_MULTICAST_LOOP));

    assertNull(dc.getLocalAddress());
    assertTrue(dc.isOpen());
    assertFalse(dc.isConnected());

    dc.close();
  }

  public void test_bind_null() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    assertNotNull(dc.getLocalAddress());
    assertTrue(dc.isOpen());
    assertFalse(dc.isConnected());

    dc.close();
    try {
      dc.getLocalAddress();
      fail();
    } catch (ClosedChannelException expected) {
    }
    assertFalse(dc.isOpen());
    assertFalse(dc.isConnected());
  }

  public void test_joinAnySource_afterClose() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    dc.close();
    try {
      dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface);
      fail();
    } catch (ClosedChannelException expected) {
    }
  }

  public void test_joinAnySource_nullGroupAddress() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    try {
      dc.join(null, ipv4networkInterface);
      fail();
    } catch (NullPointerException expected) {
    }
    dc.close();
  }

  public void test_joinAnySource_nullNetworkInterface() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    try {
      dc.join(GOOD_MULTICAST_IPv4, null);
      fail();
    } catch (NullPointerException expected) {
    }
    dc.close();
  }

  public void test_joinAnySource_nonMulticastGroupAddress_IPv4() throws Exception {
    for (NetworkInterface networkInterface : ipv4networkInterfaces) {
      test_joinAnySource_illegalArgumentExpected(networkInterface, UNICAST_IPv4_1);
    }
  }

  public void test_joinAnySource_nonMulticastGroupAddress_IPv6() throws Exception {
    for (NetworkInterface networkInterface : ipv6networkInterfaces) {
      test_joinAnySource_illegalArgumentExpected(networkInterface, UNICAST_IPv6_1);
    }
  }

  private void test_joinAnySource_illegalArgumentExpected(NetworkInterface networkInterface,
      InetAddress group) throws Exception {
    DatagramChannel dc = createReceiverChannel();
    try {
      dc.join(group, networkInterface);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    dc.close();
  }

  public void test_joinAnySource_IPv4() throws Exception {
    test_joinAnySource(ipv4networkInterfaces, GOOD_MULTICAST_IPv4, BAD_MULTICAST_IPv4);
  }

  public void test_joinAnySource_IPv6() throws Exception {
    test_joinAnySource(ipv6networkInterfaces, GOOD_MULTICAST_IPv6, BAD_MULTICAST_IPv6);
  }

  private void test_joinAnySource(
      Iterable<NetworkInterface> networkInterfaces, InetAddress group, InetAddress group2)
      throws Exception {
    for (NetworkInterface networkInterface : networkInterfaces) {
      // Set up a receiver join the group on networkInterface.
      DatagramChannel receiverChannel = createReceiverChannel();
      InetSocketAddress localAddress = (InetSocketAddress) receiverChannel.getLocalAddress();
      receiverChannel.join(group, networkInterface);

      // Send a message to the group we joined.
      String msg = "Hello World";
      sendMessage(group, localAddress.getPort(), msg, networkInterface);

      // Now verify that we received the data as expected.
      ByteBuffer recvBuffer = ByteBuffer.allocate(100);
      SocketAddress sourceAddress = receiverChannel.receive(recvBuffer);
      assertNotNull(sourceAddress);
      assertEquals(msg, new String(recvBuffer.array(), 0, recvBuffer.position()));

      // Send a message to the group we did not join.
      String msg2 = "Hello World - Different Group";
      sendMessage(group2, localAddress.getPort(), msg2, networkInterface);
      recvBuffer.position(0);
      // Now verify that we didn't receive the second message.
      SocketAddress sourceAddress2 = receiverChannel.receive(recvBuffer);
      assertNull(sourceAddress2);

      receiverChannel.close();
    }
  }

  public void test_joinAnySource_processLimit() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    for (byte i = 1; i <= 25; i++) {
      InetAddress groupAddress = Inet4Address.getByName("239.255.0." + i);
      try {
        dc.join(groupAddress, ipv4networkInterface);
      } catch (SocketException e) {
        // There is a limit, that's ok according to the RI docs. For this test a lower bound of 20
        // is used, which appears to be the default linux limit.
        // See /proc/sys/net/ipv4/igmp_max_memberships.
        assertTrue(i > 20);
        break;
      }
    }

    dc.close();
  }

  public void test_joinAnySource_blockLimit() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    MembershipKey key = dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface);
    for (byte i = 1; i <= 15; i++) {
      InetAddress sourceAddress = Inet4Address.getByName("10.0.0." + i);
      try {
        key.block(sourceAddress);
      } catch (SocketException e) {
        // There is a limit, that's ok according to the RI docs. For this test a lower bound of 10
        // is used, which appears to be the default linux limit.
        // See /proc/sys/net/ipv4/igmp_max_msf.
        assertTrue(i > 10);
        break;
      }
    }

    dc.close();
  }

  /** Confirms that calling join() does not cause an implicit bind() to take place. */
  public void test_joinAnySource_doesNotCauseBind_IPv4() throws Exception {
    test_joinAnySource_doesNotCauseBind(ipv4networkInterfaces, GOOD_MULTICAST_IPv4);
  }

  public void test_joinAnySource_doesNotCauseBind_IPv6() throws Exception {
    test_joinAnySource_doesNotCauseBind(ipv6networkInterfaces, GOOD_MULTICAST_IPv6);
  }

  private void test_joinAnySource_doesNotCauseBind(
      Iterable<NetworkInterface> networkInterfaces, InetAddress group) throws IOException {
    for (NetworkInterface networkInterface : networkInterfaces) {
      DatagramChannel dc = DatagramChannel.open();
      dc.join(group, networkInterface);
      assertNull(dc.getLocalAddress());
      dc.close();
    }
  }

  public void test_joinAnySource_networkInterfaces() throws Exception {
    // Check that we can join on specific interfaces and that we only receive if data is
    // received on that interface. This test is only really useful on devices with multiple
    // non-loopback interfaces.

    ArrayList<NetworkInterface> realInterfaces = new ArrayList<NetworkInterface>();
    Enumeration<NetworkInterface> theInterfaces = NetworkInterface.getNetworkInterfaces();
    while (theInterfaces.hasMoreElements()) {
      NetworkInterface thisInterface = theInterfaces.nextElement();
      // Skip interfaces that do not support multicast - there's no point in proving they cannot
      // send / receive multicast messages.
      if (willWorkForMulticast(thisInterface)) {
        realInterfaces.add(thisInterface);
      }
    }

    for (NetworkInterface thisInterface : realInterfaces) {
      // Get the first address on the interface.
      Enumeration<InetAddress> addresses = thisInterface.getInetAddresses();

      while (addresses.hasMoreElements()) {
        InetAddress listenAddress = addresses.nextElement();

        // Start a server which is joined to the group and has only asked for packets on this
        // interface.
        NetworkInterface sendingInterface;
        InetAddress group;
        if (listenAddress instanceof Inet4Address) {
          group = GOOD_MULTICAST_IPv4;
          sendingInterface = ipv4networkInterface;
        } else {
          group = GOOD_MULTICAST_IPv6;
          sendingInterface = ipv6networkInterface;
        }
        DatagramChannel dc = createReceiverChannel();
        InetSocketAddress localAddress = (InetSocketAddress) dc.getLocalAddress();
        dc.join(group, thisInterface);

        // Now send out a packet on sendingInterface. We should only see the packet if we send
        // it on the same interface we are listening on (thisInterface).
        String msg = "Hello World - Again " + thisInterface.getName();
        sendMessage(group, localAddress.getPort(), msg, sendingInterface);

        ByteBuffer recvBuffer = ByteBuffer.allocate(100);
        SocketAddress sourceAddress = dc.receive(recvBuffer);
        if (thisInterface.equals(sendingInterface)) {
          assertEquals(msg, new String(recvBuffer.array(), 0, recvBuffer.position()));
        } else {
          assertNull(sourceAddress);
        }

        dc.close();
      }
    }
  }

  /** Confirms that the scope of each membership is network interface-level. */
  public void test_join_canMixJoinTypesOnDifferentInterfaces() throws Exception {
    DatagramChannel dc = DatagramChannel.open();
    MembershipKey membershipKey1 = dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface);
    MembershipKey membershipKey2 = dc.join(GOOD_MULTICAST_IPv4, loopbackInterface, UNICAST_IPv4_1);
    assertNotSame(membershipKey1, membershipKey2);

    dc.close();
  }

  public void test_joinAnySource_multiple_joins_IPv4()
      throws Exception {
    test_joinAnySource_multiple_joins(ipv4networkInterfaces, GOOD_MULTICAST_IPv4);
  }

  public void test_joinAnySource_multiple_joins_IPv6()
      throws Exception {
    test_joinAnySource_multiple_joins(ipv6networkInterfaces, GOOD_MULTICAST_IPv6);
  }

  private void test_joinAnySource_multiple_joins(
      Iterable<NetworkInterface> networkInterfaces, InetAddress group) throws Exception {
    for (NetworkInterface networkInterface : networkInterfaces) {
      DatagramChannel dc = createReceiverChannel();

      MembershipKey membershipKey1 = dc.join(group, networkInterface);

      MembershipKey membershipKey2 = dc.join(group, loopbackInterface);
      assertFalse(membershipKey1.equals(membershipKey2));

      MembershipKey membershipKey1_2 = dc.join(group, networkInterface);
      assertEquals(membershipKey1, membershipKey1_2);

      dc.close();
    }
  }

  public void test_joinAnySource_multicastLoopOption_IPv4() throws Exception {
    test_joinAnySource_multicastLoopOption(ipv4networkInterfaces, GOOD_MULTICAST_IPv4);
  }

  public void test_joinAnySource_multicastLoopOption_IPv6() throws Exception {
    test_joinAnySource_multicastLoopOption(ipv6networkInterfaces, GOOD_MULTICAST_IPv6);
  }

  private void test_joinAnySource_multicastLoopOption(
      Iterable<NetworkInterface> networkInterfaces, InetAddress group) throws Exception {
    final String message = "Hello, world!";

    for (NetworkInterface networkInterface : networkInterfaces) {
      DatagramChannel dc = DatagramChannel.open();
      configureChannelForReceiving(dc);
      dc.bind(null /* leave the OS to determine the port, and use the wildcard address */);

      // Make sure that the sent packets will be sent via the interface we will be joining with.
      dc.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
      dc.join(group, networkInterface);

      // Test with loop on.
      dc.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, true /* enable loop */);

      InetSocketAddress localAddress = (InetSocketAddress) dc.getLocalAddress();

      // Send the datagram.
      sendMessage(dc, message, new InetSocketAddress(group, localAddress.getPort()));

      // Receive the datagram.
      ByteBuffer recvBuffer = ByteBuffer.allocate(100);
      SocketAddress sourceAddress = dc.receive(recvBuffer);
      assertNotNull(sourceAddress);

      String recvMessage = new String(recvBuffer.array(), 0, recvBuffer.position());
      assertEquals(message, recvMessage);

      // Turn off loop.
      dc.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false /* enable loopback */);

      // Send another datagram.
      recvBuffer.position(0);
      sendMessage(dc, message, new InetSocketAddress(group, localAddress.getPort()));

      SocketAddress sourceAddress2 = dc.receive(recvBuffer);
      assertNull(sourceAddress2);

      dc.close();
    }
  }

  public void testMembershipKeyAccessors_IPv4() throws Exception {
    testMembershipKeyAccessors(ipv4networkInterfaces, GOOD_MULTICAST_IPv4);
  }

  public void testMembershipKeyAccessors_IPv6() throws Exception {
    testMembershipKeyAccessors(ipv6networkInterfaces, GOOD_MULTICAST_IPv6);
  }

  private void testMembershipKeyAccessors(
      Iterable<NetworkInterface> networkInterfaces, InetAddress group) throws Exception {
    for (NetworkInterface networkInterface : networkInterfaces) {
      DatagramChannel dc = createReceiverChannel();

      MembershipKey key = dc.join(group, networkInterface);
      assertSame(dc, key.channel());
      assertSame(group, key.group());
      assertTrue(key.isValid());
      assertSame(networkInterface, key.networkInterface());
      assertNull(key.sourceAddress());
    }
  }

  public void test_dropAnySource_twice_IPv4() throws Exception {
    test_dropAnySource_twice(ipv4networkInterfaces, GOOD_MULTICAST_IPv4);
  }

  public void test_dropAnySource_twice_IPv6() throws Exception {
    test_dropAnySource_twice(ipv6networkInterfaces, GOOD_MULTICAST_IPv6);
  }

  private void test_dropAnySource_twice(
      Iterable<NetworkInterface> networkInterfaces, InetAddress group)
      throws Exception {
    for (NetworkInterface networkInterface : networkInterfaces) {
      DatagramChannel dc = createReceiverChannel();
      MembershipKey membershipKey = dc.join(group, networkInterface);

      assertTrue(membershipKey.isValid());
      membershipKey.drop();
      assertFalse(membershipKey.isValid());

      // Try to leave a group we are no longer a member of. It should do nothing.
      membershipKey.drop();

      dc.close();
    }
  }

  public void test_close_invalidatesMembershipKey() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    MembershipKey membershipKey = dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface);

    assertTrue(membershipKey.isValid());

    dc.close();

    assertFalse(membershipKey.isValid());
  }

  public void test_block_null() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    MembershipKey membershipKey = dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface);
    try {
      membershipKey.block(null);
      fail();
    } catch (NullPointerException expected) {
    }

    dc.close();
  }

  public void test_block_mixedAddressTypes_IPv4() throws Exception {
    test_block_illegalArgument(ipv4networkInterfaces, GOOD_MULTICAST_IPv4, UNICAST_IPv6_1);
  }

  public void test_block_mixedAddressTypes_IPv6() throws Exception {
    test_block_illegalArgument(ipv6networkInterfaces, GOOD_MULTICAST_IPv6, UNICAST_IPv4_1);
  }

  private void test_block_illegalArgument(
      Iterable<NetworkInterface> networkInterfaces, InetAddress groupAddress,
      InetAddress badBlockAddress) throws Exception {

    for (NetworkInterface networkInterface : networkInterfaces) {
      DatagramChannel dc = createReceiverChannel();
      MembershipKey membershipKey = dc.join(groupAddress, networkInterface);
      try {
        membershipKey.block(badBlockAddress);
        fail();
      } catch (IllegalArgumentException expected) {
      }

      dc.close();
    }
  }

  public void test_block_cannotBlockWithSourceSpecificMembership() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    MembershipKey membershipKey =
        dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface, UNICAST_IPv4_1);
    try {
      membershipKey.block(UNICAST_IPv4_2);
      fail();
    } catch (IllegalStateException expected) {
    }

    dc.close();
  }

  public void test_block_multipleBlocksIgnored() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    MembershipKey membershipKey = dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface);
    membershipKey.block(UNICAST_IPv4_1);

    MembershipKey membershipKey2 = membershipKey.block(UNICAST_IPv4_1);
    assertSame(membershipKey2, membershipKey);

    dc.close();
  }

  public void test_block_wildcardAddress() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    MembershipKey membershipKey = dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface);
    try {
      membershipKey.block(WILDCARD_IPv4);
      fail();
    } catch (IllegalArgumentException expected) {
    }

    dc.close();
  }

  public void test_unblock_multipleUnblocksFail() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    MembershipKey membershipKey = dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface);

    try {
      membershipKey.unblock(UNICAST_IPv4_1);
      fail();
    } catch (IllegalStateException expected) {
    }

    assertTrue(membershipKey.isValid());

    membershipKey.block(UNICAST_IPv4_1);
    membershipKey.unblock(UNICAST_IPv4_1);

    try {
      membershipKey.unblock(UNICAST_IPv4_1);
      fail();
    } catch (IllegalStateException expected) {
    }

    dc.close();
  }

  public void test_unblock_null() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    MembershipKey membershipKey = dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface);
    membershipKey.block(UNICAST_IPv4_1);

    try {
      membershipKey.unblock(null);
      fail();
    } catch (IllegalStateException expected) {
      // Either of these exceptions are fine.
    } catch (NullPointerException expected) {
      // Either of these exception are fine.
    }

    dc.close();
  }

  public void test_unblock_mixedAddressTypes_IPv4() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    MembershipKey membershipKey = dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface);
    try {
      membershipKey.unblock(UNICAST_IPv6_1);
      fail();
    } catch (IllegalStateException expected) {
      // Either of these exceptions are fine.
    } catch (IllegalArgumentException expected) {
      // Either of these exceptions are fine.
    }

    dc.close();
  }

  public void test_unblock_mixedAddressTypes_IPv6() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    MembershipKey membershipKey = dc.join(GOOD_MULTICAST_IPv6, ipv6networkInterface);
    try {
      membershipKey.unblock(UNICAST_IPv4_1);
      fail();
    } catch (IllegalStateException expected) {
      // Either of these exceptions are fine.
    } catch (IllegalArgumentException expected) {
      // Either of these exceptions are fine.
    }

    dc.close();
  }

  /** Checks that block() works when the receiver is bound to the multicast group address */
  public void test_block_filtersAsExpected_groupBind_IPv4() throws Exception {
    for (NetworkInterface networkInterface : ipv4networkInterfaces) {
      InetAddress ipv4LocalAddress = getLocalIpv4Address(networkInterface);
      test_block_filtersAsExpected(
          networkInterface,
          ipv4LocalAddress /* senderBindAddress */,
          GOOD_MULTICAST_IPv4 /* receiverBindAddress */,
          GOOD_MULTICAST_IPv4 /* groupAddress */);
    }
  }

  /** Checks that block() works when the receiver is bound to the multicast group address */
  public void test_block_filtersAsExpected_groupBind_IPv6() throws Exception {
    for (NetworkInterface networkInterface : ipv6networkInterfaces) {
      InetAddress ipv6LocalAddress = getLocalIpv6Address(networkInterface);
      test_block_filtersAsExpected(
          networkInterface,
          ipv6LocalAddress /* senderBindAddress */,
          GOOD_MULTICAST_IPv6 /* receiverBindAddress */,
          GOOD_MULTICAST_IPv6 /* groupAddress */);
    }
  }

  /** Checks that block() works when the receiver is bound to the "any" address */
  public void test_block_filtersAsExpected_anyBind_IPv4() throws Exception {
    for (NetworkInterface networkInterface : ipv4networkInterfaces) {
      InetAddress ipv4LocalAddress = getLocalIpv4Address(networkInterface);
      test_block_filtersAsExpected(
          networkInterface,
          ipv4LocalAddress /* senderBindAddress */,
          WILDCARD_IPv4 /* receiverBindAddress */,
          GOOD_MULTICAST_IPv4 /* groupAddress */);
    }
  }

  /** Checks that block() works when the receiver is bound to the "any" address */
  public void test_block_filtersAsExpected_anyBind_IPv6() throws Exception {
    for (NetworkInterface networkInterface : ipv6networkInterfaces) {
      InetAddress ipv6LocalAddress = getLocalIpv6Address(networkInterface);
      test_block_filtersAsExpected(
          networkInterface,
          ipv6LocalAddress /* senderBindAddress */,
          WILDCARD_IPv6 /* receiverBindAddress */,
          GOOD_MULTICAST_IPv6 /* groupAddress */);
    }
  }

  private void test_block_filtersAsExpected(
      NetworkInterface networkInterface, InetAddress senderBindAddress,
      InetAddress receiverBindAddress, InetAddress groupAddress) throws Exception {

    DatagramChannel sendingChannel = DatagramChannel.open();
    // In order to block a sender the sender's address must be known. The sendingChannel is
    // explicitly bound to a known, non-loopback address.
    sendingChannel.bind(new InetSocketAddress(senderBindAddress, 0));
    InetSocketAddress sendingAddress = (InetSocketAddress) sendingChannel.getLocalAddress();

    DatagramChannel receivingChannel = DatagramChannel.open();
    configureChannelForReceiving(receivingChannel);
    receivingChannel.bind(
        new InetSocketAddress(receiverBindAddress, 0) /* local port left to the OS to determine */);
    InetSocketAddress localReceivingAddress =
        (InetSocketAddress) receivingChannel.getLocalAddress();
    InetSocketAddress groupSocketAddress =
        new InetSocketAddress(groupAddress, localReceivingAddress.getPort());
    MembershipKey membershipKey =
        receivingChannel.join(groupSocketAddress.getAddress(), networkInterface);

    ByteBuffer receiveBuffer = ByteBuffer.allocate(10);

    // Send a message. It should be received.
    String msg1 = "Hello1";
    sendMessage(sendingChannel, msg1, groupSocketAddress);
    InetSocketAddress sourceAddress1 = (InetSocketAddress) receivingChannel.receive(receiveBuffer);
    assertEquals(sendingAddress, sourceAddress1);
    assertEquals(msg1, new String(receiveBuffer.array(), 0, receiveBuffer.position()));

    // Now block the sender.
    membershipKey.block(sendingAddress.getAddress());

    // Send a message. It should be filtered.
    String msg2 = "Hello2";
    sendMessage(sendingChannel, msg2, groupSocketAddress);
    receiveBuffer.position(0);
    InetSocketAddress sourceAddress2 = (InetSocketAddress) receivingChannel.receive(receiveBuffer);
    assertNull(sourceAddress2);

    // Now unblock the sender.
    membershipKey.unblock(sendingAddress.getAddress());

    // Send a message. It should be received.
    String msg3 = "Hello3";
    sendMessage(sendingChannel, msg3, groupSocketAddress);
    receiveBuffer.position(0);
    InetSocketAddress sourceAddress3 = (InetSocketAddress) receivingChannel.receive(receiveBuffer);
    assertEquals(sourceAddress3, sendingAddress);
    assertEquals(msg3, new String(receiveBuffer.array(), 0, receiveBuffer.position()));

    sendingChannel.close();
    receivingChannel.close();
  }

  public void test_joinSourceSpecific_nullGroupAddress() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    try {
      dc.join(null, ipv4networkInterface, UNICAST_IPv4_1);
      fail();
    } catch (NullPointerException expected) {
    }
    dc.close();
  }

  public void test_joinSourceSpecific_afterClose() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    dc.close();
    try {
      dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface, UNICAST_IPv4_1);
      fail();
    } catch (ClosedChannelException expected) {
    }
  }

  public void test_joinSourceSpecific_nullNetworkInterface() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    try {
      dc.join(GOOD_MULTICAST_IPv4, null, UNICAST_IPv4_1);
      fail();
    } catch (NullPointerException expected) {
    }
    dc.close();
  }

  public void test_joinSourceSpecific_nonMulticastGroupAddress_IPv4() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    try {
      dc.join(UNICAST_IPv4_1, ipv4networkInterface, UNICAST_IPv4_1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    dc.close();
  }

  public void test_joinSourceSpecific_nonMulticastGroupAddress_IPv6() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    try {
      dc.join(UNICAST_IPv6_1, ipv6networkInterface, UNICAST_IPv6_1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    dc.close();
  }

  public void test_joinSourceSpecific_nullSourceAddress_IPv4() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    try {
      dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface, null);
      fail();
    } catch (NullPointerException expected) {
    }
    dc.close();
  }

  public void test_joinSourceSpecific_nullSourceAddress_IPv6() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    try {
      dc.join(GOOD_MULTICAST_IPv6, ipv6networkInterface, null);
      fail();
    } catch (NullPointerException expected) {
    }
    dc.close();
  }

  public void test_joinSourceSpecific_mixedAddressTypes() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    try {
      dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface, UNICAST_IPv6_1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      dc.join(GOOD_MULTICAST_IPv6, ipv6networkInterface, UNICAST_IPv4_1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    dc.close();
  }

  public void test_joinSourceSpecific_nonUnicastSourceAddress_IPv4() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    try {
      dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface, BAD_MULTICAST_IPv4);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    dc.close();
  }

  public void test_joinSourceSpecific_nonUnicastSourceAddress_IPv6() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    try {
      dc.join(GOOD_MULTICAST_IPv6, ipv6networkInterface, BAD_MULTICAST_IPv6);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    dc.close();
  }

  public void test_joinSourceSpecific_multipleSourceAddressLimit() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    for (byte i = 1; i <= 20; i++) {
      InetAddress sourceAddress = Inet4Address.getByAddress(new byte[] { 10, 0, 0, i});
      try {
        dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface, sourceAddress);
      } catch (SocketException e) {
        // There is a limit, that's ok according to the RI docs. For this test a lower bound of 10
        // is used, which appears to be the default linux limit. See /proc/sys/net/ipv4/igmp_max_msf
        assertTrue(i > 10);
        break;
      }
    }

    dc.close();
  }

  /**
   * Checks that a source-specific join() works when the receiver is bound to the multicast group
   * address
   */
  public void test_joinSourceSpecific_groupBind_IPv4() throws Exception {
    for (NetworkInterface networkInterface : ipv4networkInterfaces) {
      InetAddress ipv4LocalAddress = getLocalIpv4Address(networkInterface);
      test_joinSourceSpecific(
          networkInterface,
          ipv4LocalAddress /* senderBindAddress */,
          GOOD_MULTICAST_IPv4 /* receiverBindAddress */,
          GOOD_MULTICAST_IPv4 /* groupAddress */,
          UNICAST_IPv4_1 /* badSenderAddress */);
    }
  }

  /**
   * Checks that a source-specific join() works when the receiver is bound to the multicast group
   * address
   */
  public void test_joinSourceSpecific_groupBind_IPv6() throws Exception {
    for (NetworkInterface networkInterface : ipv6networkInterfaces) {
      InetAddress ipv6LocalAddress = getLocalIpv6Address(networkInterface);
      test_joinSourceSpecific(
          networkInterface,
          ipv6LocalAddress /* senderBindAddress */,
          GOOD_MULTICAST_IPv6 /* receiverBindAddress */,
          GOOD_MULTICAST_IPv6 /* groupAddress */,
          UNICAST_IPv6_1 /* badSenderAddress */);
    }
  }

  /** Checks that a source-specific join() works when the receiver is bound to the "any" address */
  public void test_joinSourceSpecific_anyBind_IPv4() throws Exception {
    for (NetworkInterface networkInterface : ipv4networkInterfaces) {
      InetAddress ipv4LocalAddress = getLocalIpv4Address(networkInterface);
      test_joinSourceSpecific(
          networkInterface,
          ipv4LocalAddress /* senderBindAddress */,
          WILDCARD_IPv4 /* receiverBindAddress */,
          GOOD_MULTICAST_IPv4 /* groupAddress */,
          UNICAST_IPv4_1 /* badSenderAddress */);
    }
  }

  /** Checks that a source-specific join() works when the receiver is bound to the "any" address */
  public void test_joinSourceSpecific_anyBind_IPv6() throws Exception {
    for (NetworkInterface networkInterface : ipv6networkInterfaces) {
      InetAddress ipv6LocalAddress = getLocalIpv6Address(networkInterface);
      test_joinSourceSpecific(
          networkInterface,
          ipv6LocalAddress /* senderBindAddress */,
          WILDCARD_IPv6 /* receiverBindAddress */,
          GOOD_MULTICAST_IPv6 /* groupAddress */,
          UNICAST_IPv6_1 /* badSenderAddress */);
    }
  }

  /**
   * Checks that the source-specific membership is correctly source-filtered.
   *
   * @param networkInterface the network interface to use when joining
   * @param senderBindAddress the address to bind the sender socket to
   * @param receiverBindAddress the address to bind the receiver socket to
   * @param groupAddress the group address to join
   * @param badSenderAddress a unicast address to join to perform a negative test
   */
  private void test_joinSourceSpecific(
      NetworkInterface networkInterface, InetAddress senderBindAddress,
      InetAddress receiverBindAddress, InetAddress groupAddress, InetAddress badSenderAddress)
      throws Exception {
    DatagramChannel sendingChannel = DatagramChannel.open();
    // In order to be source-specific the sender's address must be known. The sendingChannel is
    // explicitly bound to a known, non-loopback address.
    sendingChannel.bind(new InetSocketAddress(senderBindAddress, 0));
    InetSocketAddress sendingAddress = (InetSocketAddress) sendingChannel.getLocalAddress();

    DatagramChannel receivingChannel = DatagramChannel.open();
    configureChannelForReceiving(receivingChannel);
    receivingChannel.bind(
        new InetSocketAddress(receiverBindAddress, 0) /* local port left to the OS to determine */);

    InetSocketAddress localReceivingAddress =
        (InetSocketAddress) receivingChannel.getLocalAddress();
    InetSocketAddress groupSocketAddress =
        new InetSocketAddress(groupAddress, localReceivingAddress.getPort());
    MembershipKey membershipKey1 = receivingChannel
        .join(groupSocketAddress.getAddress(), networkInterface, senderBindAddress);

    ByteBuffer receiveBuffer = ByteBuffer.allocate(10);

    // Send a message. It should be received.
    String msg1 = "Hello1";
    sendMessage(sendingChannel, msg1, groupSocketAddress);
    InetSocketAddress sourceAddress1 = (InetSocketAddress) receivingChannel.receive(receiveBuffer);
    assertEquals(sourceAddress1, sendingAddress);
    assertEquals(msg1, new String(receiveBuffer.array(), 0, receiveBuffer.position()));

    membershipKey1.drop();

    receivingChannel.join(groupSocketAddress.getAddress(), networkInterface, badSenderAddress);

    // Send a message. It should not be received.
    String msg2 = "Hello2";
    sendMessage(sendingChannel, msg2, groupSocketAddress);
    InetSocketAddress sourceAddress2 = (InetSocketAddress) receivingChannel.receive(receiveBuffer);
    assertNull(sourceAddress2);

    receivingChannel.close();
    sendingChannel.close();
  }

  public void test_dropSourceSpecific_twice_IPv4() throws Exception {
    for (NetworkInterface networkInterface : ipv4networkInterfaces) {
      test_dropSourceSpecific_twice(
          networkInterface,
          GOOD_MULTICAST_IPv4 /* groupAddress */,
          UNICAST_IPv4_1 /* sourceAddress */);
    }
  }

  public void test_dropSourceSpecific_twice_IPv6() throws Exception {
    for (NetworkInterface networkInterface : ipv6networkInterfaces) {
      test_dropSourceSpecific_twice(
          networkInterface,
          GOOD_MULTICAST_IPv6 /* groupAddress */,
          UNICAST_IPv6_1 /* sourceAddress */);
    }
  }

  private void test_dropSourceSpecific_twice(
      NetworkInterface networkInterface, InetAddress groupAddress, InetAddress sourceAddress)
      throws Exception {
    DatagramChannel dc = createReceiverChannel();
    MembershipKey membershipKey = dc.join(groupAddress, networkInterface, sourceAddress);

    assertTrue(membershipKey.isValid());
    membershipKey.drop();
    assertFalse(membershipKey.isValid());

    // Try to leave a group we are no longer a member of. It should do nothing.
    membershipKey.drop();

    dc.close();
  }

  public void test_dropSourceSpecific_sourceKeysAreIndependent_IPv4() throws Exception {
    for (NetworkInterface networkInterface : ipv4networkInterfaces) {
      test_dropSourceSpecific_sourceKeysAreIndependent(
          networkInterface,
          GOOD_MULTICAST_IPv4 /* groupAddress */,
          UNICAST_IPv4_1 /* sourceAddress1 */,
          UNICAST_IPv4_2 /* sourceAddress2 */);
    }
  }

  public void test_dropSourceSpecific_sourceKeysAreIndependent_IPv6() throws Exception {
    for (NetworkInterface networkInterface : ipv6networkInterfaces) {
      test_dropSourceSpecific_sourceKeysAreIndependent(
          networkInterface,
          GOOD_MULTICAST_IPv6 /* groupAddress */,
          UNICAST_IPv6_1 /* sourceAddress1 */,
          UNICAST_IPv6_2 /* sourceAddress2 */);
    }
  }

  private void test_dropSourceSpecific_sourceKeysAreIndependent(
      NetworkInterface networkInterface, InetAddress groupAddress, InetAddress sourceAddress1,
      InetAddress sourceAddress2)
      throws Exception {
    DatagramChannel dc = createReceiverChannel();
    MembershipKey membershipKey1 = dc.join(groupAddress, networkInterface, sourceAddress1);
    MembershipKey membershipKey2 = dc.join(groupAddress, networkInterface, sourceAddress2);
    assertFalse(membershipKey1.equals(membershipKey2));
    assertTrue(membershipKey1.isValid());
    assertTrue(membershipKey2.isValid());

    membershipKey1.drop();

    assertFalse(membershipKey1.isValid());
    assertTrue(membershipKey2.isValid());

    dc.close();
  }

  public void test_drop_keyBehaviorAfterDrop() throws Exception {
    DatagramChannel dc = createReceiverChannel();
    MembershipKey membershipKey =
        dc.join(GOOD_MULTICAST_IPv4, ipv4networkInterface, UNICAST_IPv4_1);
    membershipKey.drop();
    assertFalse(membershipKey.isValid());

    try {
      membershipKey.block(UNICAST_IPv4_1);
    } catch (IllegalStateException expected) {
    }

    try {
      membershipKey.unblock(UNICAST_IPv4_1);
    } catch (IllegalStateException expected) {
    }

    assertSame(dc, membershipKey.channel());
    assertSame(GOOD_MULTICAST_IPv4, membershipKey.group());
    assertSame(UNICAST_IPv4_1, membershipKey.sourceAddress());
    assertSame(ipv4networkInterface, membershipKey.networkInterface());
  }

  private static DatagramChannel createReceiverChannel() throws Exception {
    DatagramChannel dc = DatagramChannel.open();
    configureChannelForReceiving(dc);
    dc.bind(null /* leave the OS to determine the port, and use the wildcard address */);
    return dc;
  }

  private static void configureChannelForReceiving(DatagramChannel receivingChannel)
      throws Exception {

    // NOTE: At the time of writing setSoTimeout() has no effect in the RI, making these tests hang
    // if the channel is in blocking mode.
    receivingChannel.socket().setSoTimeout(200);
    receivingChannel.configureBlocking(true);
    // configureBlocking(false) can be used instead in the RI and we rely on the network to the
    // local host being instantaneous.
    // receivingChannel.configureBlocking(false);
  }

  private static boolean willWorkForMulticast(NetworkInterface iface) throws IOException {
    return iface.isUp()
        // Typically loopback interfaces do not support multicast, but they are ruled out
        // explicitly here anyway.
        && !iface.isLoopback() && iface.supportsMulticast()
        && iface.getInetAddresses().hasMoreElements();
  }

  private static void sendMessage(
      InetAddress targetGroup, int targetPort, String msg, NetworkInterface sendingInterface)
      throws IOException {
    // Any datagram socket can send to a group. It does not need to have joined the group.
    DatagramChannel dc = DatagramChannel.open();
    dc.setOption(StandardSocketOptions.IP_MULTICAST_IF, sendingInterface);
    sendMessage(dc, msg, new InetSocketAddress(targetGroup, targetPort));
    dc.close();
  }

  private static void sendMessage(
      DatagramChannel sendingChannel, String msg, InetSocketAddress targetAddress)
      throws IOException {

    ByteBuffer sendBuffer = ByteBuffer.wrap(msg.getBytes());
    sendingChannel.send(sendBuffer, targetAddress);
  }

  private static InetAddress getLocalIpv4Address(NetworkInterface networkInterface) {
    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
      if (interfaceAddress.getAddress() instanceof Inet4Address) {
        return interfaceAddress.getAddress();
      }
    }
    throw new AssertionFailedError("Unable to find local IPv4 address for " + networkInterface);
  }

  private static InetAddress getLocalIpv6Address(NetworkInterface networkInterface) {
    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
      if (interfaceAddress.getAddress() instanceof Inet6Address) {
        return interfaceAddress.getAddress();
      }
    }
    throw new AssertionFailedError("Unable to find local IPv6 address for " + networkInterface);
  }

}
