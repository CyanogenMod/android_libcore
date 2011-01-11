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

package java.net;


/**
 * Defines an interface for socket implementations to get and set socket
 * options. It is implemented by the classes {@code SocketImpl} and {@code
 * DatagramSocketImpl}.
 *
 * @see SocketImpl
 * @see DatagramSocketImpl
 */
public interface SocketOptions {
    /**
     * Number of seconds to wait when closing a socket if there
     * is still some buffered data to be sent.
     *
     * <p>If this option is set to 0, the TCP socket is closed forcefully and the
     * call to {@code close} returns immediately.
     *
     * <p>If this option is set to a value greater than 0, the value is interpreted
     * as the number of seconds to wait. If all data could be sent
     * during this time, the socket is closed normally. Otherwise the connection will be
     * closed forcefully.
     *
     * <p>Valid values for this option are in the range 0 to 65535 inclusive. (Larger
     * timeouts will be treated as 65535s timeouts; roughly 18 hours.)
     */
    public static final int SO_LINGER = 128;

    /**
     * Timeout in milliseconds for blocking accept or read/receive operations (but not
     * write/send operations). A timeout of 0 means no timeout. Negative
     * timeouts are not allowed.
     *
     * <p>An {@code InterruptedIOException} is thrown if this timeout expires.
     */
    public static final int SO_TIMEOUT = 4102;

    /**
     * Whether data is sent immediately on this socket.
     * As a side-effect this could lead to low packet efficiency. The
     * socket implementation uses the Nagle's algorithm to try to reach a higher
     * packet efficiency if this option is disabled.
     */
    public static final int TCP_NODELAY = 1;

    // For 5 and 6 see MulticastSocket

    // For 7 see PlainDatagramSocketImpl

    /**
     * The interface used to send multicast packets.
     * This option is only available on a {@link MulticastSocket}.
     */
    public static final int IP_MULTICAST_IF = 16;

    /**
     * This option can be used to set one specific interface on a multihomed
     * host on which incoming connections are accepted. It's only available on
     * server-side sockets.
     */
    public static final int SO_BINDADDR = 15;

    /**
     * This option specifies whether a reuse of a local address is allowed even
     * if an other socket is not yet removed by the operating system. It's only
     * available on a {@code MulticastSocket}.
     */
    public static final int SO_REUSEADDR = 4;

    // 10 not currently used

    /**
     * The size in bytes of a socket's send buffer. This must be an integer greater than 0.
     * This is a hint to the kernel; the kernel may use a larger buffer.
     *
     * <p>For datagram sockets, it is implementation-defined whether packets larger than
     * this size can be sent.
     */
    public static final int SO_SNDBUF = 4097;

    /**
     * The size in bytes of a socket's receive buffer. This must be an integer greater than 0.
     * This is a hint to the kernel; the kernel may use a larger buffer.
     *
     * <p>For datagram sockets, packets larger than this value will be discarded.
     */
    public static final int SO_RCVBUF = 4098;

    /**
     * This option can be used to bind a datagram socket to a
     * particular network interface.  When this is done, only packets
     * received on the specified interface will be processed by the
     * socket.  Packets sent via this socket will be transmitted by
     * the specified interface.  The argument to this operation is the
     * network-interface index.
     * @hide
     */
    public static final int SO_BINDTODEVICE = 8192;

    // For 13, see DatagramSocket

    /**
     * This option specifies whether socket implementations can send keepalive
     * messages if no data has been sent for a longer time.
     */
    public static final int SO_KEEPALIVE = 8;

    /**
     * This option specifies the value for the type-of-service field of the IPv4 header, or the
     * traffic class field of the IPv6 header. These correspond to the IP_TOS and IPV6_TCLASS
     * socket options. These may be ignored by the underlying OS. Values must be between 0 and 255
     * inclusive.
     *
     * <p>See <a href="http://www.ietf.org/rfc/rfc1349.txt">RFC 1349</a> for more about IPv4
     * and <a href="http://www.ietf.org/rfc/rfc2460.txt">RFC 2460</a> for more about IPv6.
     */
    public static final int IP_TOS = 3;

    /**
     * This option specifies whether the local loopback of multicast packets is
     * enabled or disabled. This option is enabled by default on multicast
     * sockets.
     */
    public static final int IP_MULTICAST_LOOP = 18;

    /**
     * This option can be used to enable broadcasting on datagram sockets.
     */
    public static final int SO_BROADCAST = 32;

    /**
     * This boolean option specifies whether sending TCP urgent data is supported on
     * this socket or not.
     */
    public static final int SO_OOBINLINE = 4099;

    /**
     * This option can be used to set one specific interface on a multihomed
     * host on which incoming connections are accepted. It's only available on
     * server-side sockets. This option supports setting outgoing interfaces
     * with either IPv4 or IPv6 addresses.
     */
    public static final int IP_MULTICAST_IF2 = 31;

    /**
     * Gets the value for the specified socket option.
     *
     * @return the option value.
     * @param optID
     *            the option identifier.
     * @throws SocketException
     *             if an error occurs reading the option value.
     */
    public Object getOption(int optID) throws SocketException;

    /**
     * Sets the value of the specified socket option.
     *
     * @param optID
     *            the option identifier.
     * @param val
     *            the value to be set for the option.
     * @throws SocketException
     *             if an error occurs setting the option value.
     */
    public void setOption(int optID, Object val) throws SocketException;
}
