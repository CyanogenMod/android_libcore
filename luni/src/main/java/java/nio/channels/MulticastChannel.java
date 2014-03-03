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

package java.nio.channels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * A type of {@link NetworkChannel} that supports IP multicasting. IP multicasting allows for
 * efficient routing of an IP datagram to multiple hosts. Hosts wishing to receive multicast
 * datagrams join a multicast group identified by a multicast IP address.
 *
 * <p>Any datagram socket can be used to <em>send</em> to a multicast group: senders <em>do not</em>
 * have to be a member of the group.
 *
 * <p>See <a href="http://www.ietf.org/rfc/rfc2236.txt">RFC 2236: Internet Group Management
 * Protocol, Version 2</a> and <a href="http://www.ietf.org/rfc/rfc3376.txt">RFC 3376: Internet
 * Group Management Protocol, Version 3</a> for network-level information regarding IPv4 group
 * membership. See <a href="http://www.ietf.org/rfc/rfc2710.txt">RFC 2710: Multicast Listener
 * Discovery (MLD) for IPv6</a> and <a href="http://www.ietf.org/rfc/rfc3810.txt">RFC 3810:
 * Multicast Listener Discovery Version 2 (MLDv2) for IPv6</a> for equivalent IPv6 information.
 *
 * <p>See <a href="http://tools.ietf.org/html/rfc3678">RFC 3678: Socket Interface Extensions for
 * Multicast Source Filters</a> for concepts and terminology associated with multicast membership.
 *
 * <p>IP multicast requires support from network infrastructure; networks may not support
 * all features of IP multicast.
 *
 * <p>A channel can be restricted to send multicast datagrams through a specific
 * {@link NetworkInterface} by using {@link #setOption(java.net.SocketOption, Object)} with
 * {@link java.net.StandardSocketOptions#IP_MULTICAST_IF}.
 *
 * <p>A channel may or may not receive multicast datagrams sent from this host, determined by the
 * {@link java.net.StandardSocketOptions#IP_MULTICAST_LOOP} option.
 *
 * <p>The time-to-live for multicast datagrams can be set using the
 * {@link java.net.StandardSocketOptions#IP_MULTICAST_TTL} option.
 *
 * <p>Usually multicast channels should have {@link java.net.StandardSocketOptions#SO_REUSEADDR}
 * set to {@code true} before binding to enable multiple sockets on this host to be members of
 * the same multicast group.
 *
 * <p>Typically multicast channels are {@link NetworkChannel#bind bound} to a wildcard address
 * such as "0.0.0.0" (IPv4) or "::" (IPv6). They may also be bound to a multicast group address.
 * Binding to a multicast group address restricts datagrams received to only those sent to the
 * multicast group. When the wildcard address is used the socket may join multiple groups and also
 * receive non-multicast datagrams sent directly to the host. The port the channel is bound to is
 * important: only datagrams sent to the group on that port will be received.
 *
 * <p>Having bound a channel, the group can be joined. Memberships are either "any-source" or
 * "source-specific". The type of membership is determined by the variant of {@code join} that is
 * used. See {@link #join(java.net.InetAddress, java.net.NetworkInterface)} and
 * {@link #join(java.net.InetAddress, java.net.NetworkInterface, java.net.InetAddress)} for more
 * information.
 *
 * @since 1.7
 * @hide Until ready for a public API change
 */
public interface MulticastChannel extends NetworkChannel {

  // @hide Until ready for a public API change
  // /**
  //  * {@inheritDoc}
  //  *
  //  * If the channel is currently part of one or more multicast groups then the memberships are
  // * dropped and any associated {@code MembershipKey} objects are invalidated.
  // */
  void close() throws IOException;

  /**
   * Creates an any-source membership to the {@code groupAddress} on the specified
   * {@code networkInterface}. Returns a {@code MembershipKey} that can be used to modify or
   * {@link MembershipKey#drop()} the membership. See {@link MembershipKey#block(InetAddress)} and
   * {@link MembershipKey#unblock(InetAddress)} for methods to modify source-address
   * filtering.
   *
   * <p>A channel may join several groups. Each membership is network interface-specific: an
   * application must join the group for each applicable network interface to receive datagrams.
   *
   * <p>Any-source and source-specific memberships cannot be mixed for a group address on a given
   * network interface. An {@code IllegalStateException} will be thrown if joins of different types
   * are attempted for a given {@code groupAddress}, {@code networkInterface} pair. Joining a
   * multicast group with the same arguments as an existing, valid membership returns the same
   * {@code MembershipKey}.
   *
   * <p>There is an OS-level limit on the number of multicast groups a process can join.
   * This is typically 20. Attempts to join more than this result in a {@code SocketException}.
   *
   * @param groupAddress the multicast group address to join
   * @param networkInterface the network address to join with
   * @throws IllegalArgumentException
   *         if the group address is not a multicast address or the network interface does not
   *         support multicast
   * @throws IllegalStateException
   *         if the channel already has a source-specific membership for the group/network interface
   * @throws ClosedChannelException
   *         if the channel is closed
   * @throws IOException
   *         if some other I/O error occurs
   * @hide Until ready for a public API change
   */
  MembershipKey join(InetAddress groupAddress, NetworkInterface networkInterface)
      throws IOException;

  /**
   * Creates a source-specific membership to the {@code groupAddress} on the specified
   * {@code networkInterface} filtered by the {@code sourceAddress}. Returns a
   * {@code MembershipKey} that can be used to {@link MembershipKey#drop()} the membership.
   *
   * <p>A channel may join several groups. Each membership is network interface-specific: an
   * application must join the group for each applicable network interface to receive datagrams.
   *
   * <p>Any-source and source-specific memberships cannot be mixed for a group address on a given
   * network interface. An {@code IllegalStateException} will be thrown if joins of different types
   * are attempted for a given {@code groupAddress}, {@code networkInterface} pair. Joining a
   * multicast group with the same arguments as an existing, valid membership returns the same
   * {@code MembershipKey}.
   *
   * <p>There is an OS-level limit on the number of multicast groups a process can join.
   * This is typically 20. Attempts to join more than this result in a {@code SocketException}.
   *
   * <p>There is an OS-level limit on the number of source addresses that can be joined for a given
   * {@code groupAddress}, {@code networkInterface} pair. This is typically 10. Attempts to add
   * more than this result in a {@code SocketException}.
   *
   * @param groupAddress the multicast group address to join
   * @param networkInterface the network address to join with
   * @param sourceAddress the source address to restrict datagrams to
   * @throws IllegalArgumentException
   *         if the group address is not a multicast address, the network interface does not
   *         support multicast, or the {@code groupAddress} and {@code sourceAddress} are not of
   *         compatible types
   * @throws IllegalStateException
   *         if the channel already has a source-specific membership for the group/network interface
   * @throws ClosedChannelException
   *         if the channel is closed
   * @throws IOException
   *         if some other I/O error occurs
   * @hide Until ready for a public API change
   */
  MembershipKey join(
      InetAddress groupAddress, NetworkInterface networkInterface, InetAddress sourceAddress)
      throws IOException;

}
