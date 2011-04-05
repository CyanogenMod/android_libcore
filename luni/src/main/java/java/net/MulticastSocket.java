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

import java.io.IOException;
import java.util.Enumeration;

/**
 * This class implements a multicast socket for sending and receiving IP
 * multicast datagram packets.
 *
 * @see DatagramSocket
 */
public class MulticastSocket extends DatagramSocket {
    private static final int NO_INTERFACE_INDEX = 0;
    private static final int UNSET_INTERFACE_INDEX = -1;

    private InetAddress interfaceSet;

    /**
     * Constructs a multicast socket, bound to any available port on the
     * localhost.
     *
     * @throws IOException
     *             if an error occurs creating or binding the socket.
     */
    public MulticastSocket() throws IOException {
        setReuseAddress(true);
    }

    /**
     * Constructs a multicast socket, bound to the specified port on the
     * localhost.
     *
     * @param aPort
     *            the port to bind on the localhost.
     * @throws IOException
     *             if an error occurs creating or binding the socket.
     */
    public MulticastSocket(int aPort) throws IOException {
        super(aPort);
        setReuseAddress(true);
    }

    /**
     * Gets the network address used by this socket. This is useful on
     * multihomed machines.
     *
     * @return the address of the network interface through which the datagram
     *         packets are sent or received.
     * @throws SocketException
     *                if an error occurs while getting the interface address.
     */
    public InetAddress getInterface() throws SocketException {
        checkClosedAndBind(false);
        if (interfaceSet != null) {
            return interfaceSet;
        }
        InetAddress ipvXaddress = (InetAddress) impl.getOption(SocketOptions.IP_MULTICAST_IF);
        if (ipvXaddress.isAnyLocalAddress()) {
            // the address was not set at the IPv4 level so check the IPv6
            // level
            NetworkInterface theInterface = getNetworkInterface();
            if (theInterface != null) {
                Enumeration<InetAddress> addresses = theInterface.getInetAddresses();
                if (addresses != null) {
                    while (addresses.hasMoreElements()) {
                        InetAddress nextAddress = addresses.nextElement();
                        if (nextAddress instanceof Inet6Address) {
                            return nextAddress;
                        }
                    }
                }
            }
        }
        return ipvXaddress;
    }

    /**
     * Returns the outgoing network interface used by this socket. This is useful on
     * multihomed machines.
     *
     * @throws SocketException
     *                if an error occurs while getting the interface.
     * @since 1.4
     */
    public NetworkInterface getNetworkInterface() throws SocketException {
        checkClosedAndBind(false);

        int index = (Integer) impl.getOption(SocketOptions.IP_MULTICAST_IF2);
        if (index != 0) {
            return NetworkInterface.getByIndex(index);
        }

        // This is what the RI returns for a MulticastSocket that hasn't been constrained
        // to a specific interface.
        InetAddress[] addresses;
        if (InetAddress.preferIPv6Addresses()) {
            addresses = new InetAddress[] { Inet6Address.ANY };
        } else {
            addresses = new InetAddress[] { Inet4Address.ANY };
        }
        return new NetworkInterface(null, null, addresses, UNSET_INTERFACE_INDEX);
    }

    /**
     * Gets the time-to-live (TTL) for multicast packets sent on this socket.
     *
     * @return the default value for the time-to-life field.
     * @throws IOException
     *                if an error occurs reading the default value.
     */
    public int getTimeToLive() throws IOException {
        checkClosedAndBind(false);
        return impl.getTimeToLive();
    }

    /**
     * Gets the time-to-live (TTL) for multicast packets sent on this socket.
     *
     * @return the default value for the time-to-life field.
     * @throws IOException
     *                if an error occurs reading the default value.
     * @deprecated Replaced by {@link #getTimeToLive}
     * @see #getTimeToLive()
     */
    @Deprecated
    public byte getTTL() throws IOException {
        checkClosedAndBind(false);
        return impl.getTTL();
    }

    /**
     * Adds this socket to the specified multicast group. A socket must join a
     * group before data may be received. A socket may be a member of multiple
     * groups but may join any group only once.
     *
     * @param groupAddr
     *            the multicast group to be joined.
     * @throws IOException
     *                if an error occurs while joining a group.
     */
    public void joinGroup(InetAddress groupAddr) throws IOException {
        checkJoinOrLeave(groupAddr);
        impl.join(groupAddr);
    }

    /**
     * Adds this socket to the specified multicast group. A socket must join a
     * group before data may be received. A socket may be a member of multiple
     * groups but may join any group only once.
     *
     * @param groupAddress
     *            the multicast group to be joined.
     * @param netInterface
     *            the network interface on which the datagram packets will be
     *            received.
     * @throws IOException
     *                if the specified address is not a multicast address.
     * @throws IllegalArgumentException
     *                if no multicast group is specified.
     * @since 1.4
     */
    public void joinGroup(SocketAddress groupAddress, NetworkInterface netInterface) throws IOException {
        checkJoinOrLeave(groupAddress, netInterface);
        impl.joinGroup(groupAddress, netInterface);
    }

    /**
     * Removes this socket from the specified multicast group.
     *
     * @param groupAddr
     *            the multicast group to be left.
     * @throws NullPointerException
     *                if {@code groupAddr} is {@code null}.
     * @throws IOException
     *                if the specified group address is not a multicast address.
     */
    public void leaveGroup(InetAddress groupAddr) throws IOException {
        checkJoinOrLeave(groupAddr);
        impl.leave(groupAddr);
    }

    /**
     * Removes this socket from the specified multicast group.
     *
     * @param groupAddress
     *            the multicast group to be left.
     * @param netInterface
     *            the network interface on which the addresses should be
     *            dropped.
     * @throws IOException
     *                if the specified group address is not a multicast address.
     * @throws IllegalArgumentException
     *                if {@code groupAddress} is {@code null}.
     * @since 1.4
     */
    public void leaveGroup(SocketAddress groupAddress, NetworkInterface netInterface) throws IOException {
        checkJoinOrLeave(groupAddress, netInterface);
        impl.leaveGroup(groupAddress, netInterface);
    }

    private void checkJoinOrLeave(SocketAddress groupAddress, NetworkInterface netInterface) throws IOException {
        checkClosedAndBind(false);
        if (groupAddress == null) {
            throw new IllegalArgumentException("groupAddress == null");
        }

        if ((netInterface != null) && (netInterface.getFirstAddress() == null)) {
            throw new SocketException("No address associated with interface: " + netInterface);
        }

        if (!(groupAddress instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Group address not an InetSocketAddress: " +
                    groupAddress.getClass());
        }

        InetAddress groupAddr = ((InetSocketAddress) groupAddress).getAddress();
        if (groupAddr == null) {
            throw new SocketException("Group address has no address: " + groupAddress);
        }

        if (!groupAddr.isMulticastAddress()) {
            throw new IOException("Not a multicast group: " + groupAddr);
        }
    }

    private void checkJoinOrLeave(InetAddress groupAddr) throws IOException {
        checkClosedAndBind(false);
        if (!groupAddr.isMulticastAddress()) {
            throw new IOException("Not a multicast group: " + groupAddr);
        }
    }

    /**
     * Send the packet on this socket. The packet must satisfy the security
     * policy before it may be sent.
     *
     * @param pack
     *            the {@code DatagramPacket} to send
     * @param ttl
     *            the TTL setting for this transmission, overriding the socket
     *            default
     * @throws IOException
     *                if an error occurs while sending data or setting options.
     * @deprecated use {@link #setTimeToLive}.
     */
    @Deprecated
    public void send(DatagramPacket pack, byte ttl) throws IOException {
        checkClosedAndBind(false);
        InetAddress packAddr = pack.getAddress();
        int currTTL = getTimeToLive();
        if (packAddr.isMulticastAddress() && (byte) currTTL != ttl) {
            try {
                setTimeToLive(ttl & 0xff);
                impl.send(pack);
            } finally {
                setTimeToLive(currTTL);
            }
        } else {
            impl.send(pack);
        }
    }

    /**
     * Sets the interface address used by this socket. This allows to send
     * multicast packets on a different interface than the default interface of
     * the local system. This is useful on multihomed machines.
     *
     * @param addr
     *            the multicast interface network address to set.
     * @throws SocketException
     *                if an error occurs while setting the network interface
     *                address option.
     */
    public void setInterface(InetAddress addr) throws SocketException {
        checkClosedAndBind(false);
        if (addr == null) {
            throw new NullPointerException();
        }
        if (addr.isAnyLocalAddress()) {
            impl.setOption(SocketOptions.IP_MULTICAST_IF, Inet4Address.ANY);
        } else if (addr instanceof Inet4Address) {
            impl.setOption(SocketOptions.IP_MULTICAST_IF, addr);
            // keep the address used to do the set as we must return the same
            // value and for IPv6 we may not be able to get it back uniquely
            interfaceSet = addr;
        }

        /*
         * now we should also make sure this works for IPv6 get the network
         * interface for the address and set the interface using its index
         * however if IPv6 is not enabled then we may get an exception. if IPv6
         * is not enabled
         */
        NetworkInterface theInterface = NetworkInterface.getByInetAddress(addr);
        if ((theInterface != null) && (theInterface.getIndex() != 0)) {
            try {
                impl.setOption(SocketOptions.IP_MULTICAST_IF2, Integer.valueOf(theInterface.getIndex()));
            } catch (SocketException e) {
                // Ignored
            }
        } else if (addr.isAnyLocalAddress()) {
            try {
                impl.setOption(SocketOptions.IP_MULTICAST_IF2, Integer.valueOf(0));
            } catch (SocketException e) {
                // Ignored
            }
        } else if (addr instanceof Inet6Address) {
            throw new SocketException("Address not associated with an interface: " + addr);
        }
    }

    /**
     * Sets the network interface used by this socket. This is useful for
     * multihomed machines.
     *
     * @param netInterface
     *            the multicast network interface to set.
     * @throws SocketException
     *                if an error occurs while setting the network interface
     *                option.
     * @since 1.4
     */
    public void setNetworkInterface(NetworkInterface netInterface) throws SocketException {
        checkClosedAndBind(false);

        if (netInterface == null) {
            // throw a socket exception indicating that we do not support this
            throw new SocketException("netInterface == null");
        }

        InetAddress firstAddress = netInterface.getFirstAddress();
        if (firstAddress == null) {
            throw new SocketException("No address associated with interface: " + netInterface);
        }

        if (netInterface.getIndex() == UNSET_INTERFACE_INDEX) {
            // set the address using IP_MULTICAST_IF to make sure this
            // works for both IPv4 and IPv6
            impl.setOption(SocketOptions.IP_MULTICAST_IF, Inet4Address.ANY);

            try {
                // we have the index so now we pass set the interface
                // using IP_MULTICAST_IF2. This is what is used to set
                // the interface on systems which support IPv6
                impl.setOption(SocketOptions.IP_MULTICAST_IF2, Integer.valueOf(NO_INTERFACE_INDEX));
            } catch (SocketException e) {
                // for now just do this, -- could be narrowed?
            }
        }

        /*
         * Now try to set using IPv4 way. However, if interface passed in has no
         * IP addresses associated with it then we cannot do it. first we have
         * to make sure there is an IPv4 address that we can use to call set
         * interface otherwise we will not set it
         */
        Enumeration<InetAddress> theAddresses = netInterface.getInetAddresses();
        boolean found = false;
        firstAddress = null;
        while ((theAddresses.hasMoreElements()) && (found != true)) {
            InetAddress theAddress = theAddresses.nextElement();
            if (theAddress instanceof Inet4Address) {
                firstAddress = theAddress;
                found = true;
            }
        }
        if (netInterface.getIndex() == NO_INTERFACE_INDEX) {
            // the system does not support IPv6 and does not provide
            // indexes for the network interfaces. Just pass in the
            // first address for the network interface
            if (firstAddress != null) {
                impl.setOption(SocketOptions.IP_MULTICAST_IF, firstAddress);
            } else {
                /*
                 * we should never get here as there should not be any network
                 * interfaces which have no IPv4 address and which does not have
                 * the network interface index not set correctly
                 */
                throw new SocketException("No address associated with interface: " + netInterface);
            }
        } else {
            // set the address using IP_MULTICAST_IF to make sure this
            // works for both IPv4 and IPv6
            if (firstAddress != null) {
                impl.setOption(SocketOptions.IP_MULTICAST_IF, firstAddress);
            }

            try {
                // we have the index so now we pass set the interface
                // using IP_MULTICAST_IF2. This is what is used to set
                // the interface on systems which support IPv6
                impl.setOption(SocketOptions.IP_MULTICAST_IF2, Integer
                        .valueOf(netInterface.getIndex()));
            } catch (SocketException e) {
                // for now just do this -- could be narrowed?
            }
        }

        interfaceSet = null;
    }

    /**
     * Sets the time-to-live (TTL) for multicast packets sent on this socket.
     * Valid TTL values are between 0 and 255 inclusive.
     *
     * @throws IOException
     *                if an error occurs while setting the TTL option value.
     */
    public void setTimeToLive(int ttl) throws IOException {
        checkClosedAndBind(false);
        if (ttl < 0 || ttl > 255) {
            throw new IllegalArgumentException("TimeToLive out of bounds: " + ttl);
        }
        impl.setTimeToLive(ttl);
    }

    /**
     * Sets the time-to-live (TTL) for multicast packets sent on this socket.
     * Valid TTL values are between 0 and 255 inclusive.
     *
     * @throws IOException
     *                if an error occurs while setting the TTL option value.
     * @deprecated Replaced by {@link #setTimeToLive}
     * @see #setTimeToLive(int)
     */
    @Deprecated
    public void setTTL(byte ttl) throws IOException {
        checkClosedAndBind(false);
        impl.setTTL(ttl);
    }

    @Override
    synchronized void createSocket(int aPort, InetAddress addr) throws SocketException {
        impl = factory != null ? factory.createDatagramSocketImpl() : new PlainDatagramSocketImpl();
        impl.create();
        try {
            impl.setOption(SocketOptions.SO_REUSEADDR, Boolean.TRUE);
            impl.bind(aPort, addr);
            isBound = true;
        } catch (SocketException e) {
            close();
            throw e;
        }
    }

    /**
     * Constructs a {@code MulticastSocket} bound to the host/port specified by
     * the {@code SocketAddress}, or an unbound {@code DatagramSocket} if the
     * {@code SocketAddress} is {@code null}.
     *
     * @param localAddr
     *            the local machine address and port to bind to.
     * @throws IllegalArgumentException
     *             if the {@code SocketAddress} is not supported.
     * @throws IOException
     *             if an error occurs creating or binding the socket.
     * @since 1.4
     */
    public MulticastSocket(SocketAddress localAddr) throws IOException {
        super(localAddr);
        setReuseAddress(true);
    }

    /**
     * Gets the state of the {@code SocketOptions.IP_MULTICAST_LOOP}.
     *
     * @return {@code true} if the IP multicast loop is enabled, {@code false}
     *         otherwise.
     * @throws SocketException
     *             if the socket is closed or the option is invalid.
     * @since 1.4
     */
    public boolean getLoopbackMode() throws SocketException {
        checkClosedAndBind(false);
        return !((Boolean) impl.getOption(SocketOptions.IP_MULTICAST_LOOP)).booleanValue();
    }

    /**
     * Sets the {@link SocketOptions#IP_MULTICAST_LOOP}.
     *
     * @param disable
     *            true to <i>disable</i> loopback
     * @throws SocketException
     *             if the socket is closed or the option is invalid.
     * @since 1.4
     */
    public void setLoopbackMode(boolean disable) throws SocketException {
        checkClosedAndBind(false);
        impl.setOption(SocketOptions.IP_MULTICAST_LOOP, Boolean.valueOf(!disable));
    }
}
