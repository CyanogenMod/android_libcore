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
 * A token produced as the result of joining a multicast group with
 * {@link DatagramChannel#join(java.net.InetAddress, java.net.NetworkInterface)} or
 * {@link DatagramChannel#join(java.net.InetAddress, java.net.NetworkInterface,
 * java.net.InetAddress)}.
 *
 * <p>A multicast group membership can be source-specific or any-source. Source-specific memberships
 * only allow datagrams from a single source address to be received. Any-source memberships
 * initially allow datagrams from any source address to be received, but may have individual unicast
 * IP addresses blocked via {@link #block(java.net.InetAddress)}. Any-source membership keys return
 * {@code null} from {@link #sourceAddress()}.
 *
 * <p>See <a href="http://tools.ietf.org/html/rfc3678">RFC 3678: Socket Interface Extensions for
 * Multicast Source Filters</a> for concepts and terminology associated with multicast membership.
 *
 * @since 1.7
 * @hide Until ready for a public API change
 */
public abstract class MembershipKey {

  protected MembershipKey() {}

  /**
   * Returns {@code true} until the membership is dropped with {@link #drop()} or the associated
   * channel is closed.
   */
  public abstract boolean isValid();

  /**
   * Drops this membership from the multicast group, invalidating this key.
   */
  public abstract void drop();

  /**
   * Blocks datagrams from the specified source address; after this call any datagrams received from
   * the address will be discarded. Blocking an already-blocked source address has no effect. A
   * blocked address can be unblocked by calling {@link #unblock(java.net.InetAddress)}.
   *
   * <p>The block may not take effect instantaneously: datagrams that are already buffered by the
   * underlying OS may still be delivered.
   *
   * <p>There is an OS-level limit on the number of source addresses that can be block for a given
   * {@code groupAddress}, {@code networkInterface} pair. This is typically 10. Attempts to add
   * more than this result in a {@code SocketException}.
   *
   * <p>If this membership key is source-specific an {@link IllegalStateException} is thrown.
   *
   * @throws IllegalStateException
   *         if this membership key is no longer valid or is of the wrong type
   * @throws IllegalArgumentException
   *         if the source address is not unicast address of the same type as the multicast group
   *         address supplied when the group was joined
   * @throws IOException
   *         if an I/O error occurs.
   */
  public abstract MembershipKey block(InetAddress source) throws IOException;

  /**
   * Unblocks datagrams from the specified source address that were previously blocked with a call
   * to {@link #block(java.net.InetAddress)}; after this call any datagrams received from the
   * address will be received. Unblocking an address that is not currently blocked throws an
   * {@code IllegalStateException}.
   *
   * <p>If this membership key is source-specific an {@link IllegalStateException} is thrown.
   *
   * @throws IllegalStateException
   *         if this membership key is no longer valid or is of the wrong type, or the address is
   *         not currently blocked
   * @throws IllegalArgumentException
   *         if the source address is not unicast address of the same type as the multicast group
   *         address supplied when the group was joined
   */
  public abstract MembershipKey unblock(InetAddress source);

  /**
   * Returns the {@code MulticastChannel} associated with this key. Continues returning the value
   * even when the key has been invalidated.
   */
  public abstract MulticastChannel channel();

  /**
   * Returns the multicast group address associated with this key. Continues returning the value
   * even when the key has been invalidated.
   */
  public abstract InetAddress group();

  /**
   * Returns the network interface associated with this key. Continues returning the value
   * even when the key has been invalidated.
   */
  public abstract NetworkInterface networkInterface();

  /**
   * Returns the source address associated with this key if the membership is source-specific.
   * Returns {@code null} if the membership is any-source. Continues returning the value
   * even when the key has been invalidated.
   */
  public abstract InetAddress sourceAddress();
}
