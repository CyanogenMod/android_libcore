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

import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import libcore.io.IoUtils;
import libcore.io.StructGroupReq;
import libcore.util.EmptyArray;
import org.apache.harmony.luni.platform.Platform;

/**
 * @hide used in java.nio.
 */
public class PlainDatagramSocketImpl extends DatagramSocketImpl {

    private volatile boolean isNativeConnected;

    private final CloseGuard guard = CloseGuard.get();

    /**
     * used to keep address to which the socket was connected to at the native
     * level
     */
    private InetAddress connectedAddress;

    private int connectedPort = -1;

    public PlainDatagramSocketImpl(FileDescriptor fd, int localPort) {
        this.fd = fd;
        this.localPort = localPort;
        if (fd.valid()) {
            guard.open("close");
        }
    }

    public PlainDatagramSocketImpl() {
        fd = new FileDescriptor();
    }

    @Override
    public void bind(int port, InetAddress addr) throws SocketException {
        Platform.NETWORK.bind(fd, addr, port);
        if (port != 0) {
            localPort = port;
        } else {
            localPort = IoUtils.getSocketLocalPort(fd);
        }

        try {
            setOption(SocketOptions.SO_BROADCAST, Boolean.TRUE);
        } catch (IOException ignored) {
        }
    }

    @Override
    public synchronized void close() {
        guard.close();
        try {
            Platform.NETWORK.close(fd);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void create() throws SocketException {
        this.fd = IoUtils.socket(false);
    }

    @Override protected void finalize() throws Throwable {
        try {
            if (guard != null) {
                guard.warnIfOpen();
            }
            close();
        } finally {
            super.finalize();
        }
    }

    @Override public Object getOption(int option) throws SocketException {
        return IoUtils.getSocketOption(fd, option);
    }

    @Override
    public int getTimeToLive() throws IOException {
        return (Integer) getOption(IoUtils.JAVA_IP_MULTICAST_TTL);
    }

    @Override
    public byte getTTL() throws IOException {
        return (byte) getTimeToLive();
    }

    private static StructGroupReq makeGroupReq(InetAddress gr_group, NetworkInterface networkInterface) {
        int gr_interface = (networkInterface != null) ? networkInterface.getIndex() : 0;
        return new StructGroupReq(gr_interface, gr_group);
    }

    @Override
    public void join(InetAddress addr) throws IOException {
        setOption(IoUtils.JAVA_MCAST_JOIN_GROUP, makeGroupReq(addr, null));
    }

    @Override
    public void joinGroup(SocketAddress addr, NetworkInterface netInterface) throws IOException {
        if (addr instanceof InetSocketAddress) {
            InetAddress groupAddr = ((InetSocketAddress) addr).getAddress();
            setOption(IoUtils.JAVA_MCAST_JOIN_GROUP, makeGroupReq(groupAddr, netInterface));
        }
    }

    @Override
    public void leave(InetAddress addr) throws IOException {
        setOption(IoUtils.JAVA_MCAST_LEAVE_GROUP, makeGroupReq(addr, null));
    }

    @Override
    public void leaveGroup(SocketAddress addr, NetworkInterface netInterface) throws IOException {
        if (addr instanceof InetSocketAddress) {
            InetAddress groupAddr = ((InetSocketAddress) addr).getAddress();
            setOption(IoUtils.JAVA_MCAST_LEAVE_GROUP, makeGroupReq(groupAddr, netInterface));
        }
    }

    @Override
    protected int peek(InetAddress sender) throws IOException {
        // We don't actually want the data: we just want the DatagramPacket's filled-in address.
        DatagramPacket packet = new DatagramPacket(EmptyArray.BYTE, 0);
        int result = peekData(packet);
        // TODO: maybe recv should do this?
        sender.ipaddress = packet.getAddress().getAddress();
        return result;
    }

    private void doRecv(DatagramPacket pack, boolean peek) throws IOException {
        Platform.NETWORK.recv(fd, pack, pack.getData(), pack.getOffset(), pack.getLength(), peek,
                isNativeConnected);
        if (isNativeConnected) {
            updatePacketRecvAddress(pack);
        }
    }

    @Override
    public void receive(DatagramPacket pack) throws IOException {
        doRecv(pack, false);
    }

    @Override
    public int peekData(DatagramPacket pack) throws IOException {
        doRecv(pack, true);
        return pack.getPort();
    }

    @Override
    public void send(DatagramPacket packet) throws IOException {
        int port = isNativeConnected ? 0 : packet.getPort();
        InetAddress address = isNativeConnected ? null : packet.getAddress();
        Platform.NETWORK.send(fd, packet.getData(), packet.getOffset(), packet.getLength(),
                              port, address);
    }

    public void setOption(int option, Object value) throws SocketException {
        IoUtils.setSocketOption(fd, option, value);
    }

    @Override
    public void setTimeToLive(int ttl) throws IOException {
        setOption(IoUtils.JAVA_IP_MULTICAST_TTL, Integer.valueOf(ttl));
    }

    @Override
    public void setTTL(byte ttl) throws IOException {
        setTimeToLive((int) ttl & 0xff); // Avoid sign extension.
    }

    @Override
    public void connect(InetAddress inetAddr, int port) throws SocketException {
        IoUtils.connect(fd, inetAddr, port); // Throws on failure.
        try {
            connectedAddress = InetAddress.getByAddress(inetAddr.getAddress());
        } catch (UnknownHostException e) {
            // this is never expected to happen as we should not have gotten
            // here if the address is not resolvable
            throw new SocketException("Host is unresolved: " + inetAddr.getHostName());
        }
        connectedPort = port;
        isNativeConnected = true;
    }

    @Override
    public void disconnect() {
        try {
            Platform.NETWORK.disconnectDatagram(fd);
        } catch (Exception ignored) {
        }
        connectedPort = -1;
        connectedAddress = null;
        isNativeConnected = false;
    }

    /**
     * Set the received address and port in the packet. We do this when the
     * Datagram socket is connected at the native level and the
     * recvConnnectedDatagramImpl does not update the packet with address from
     * which the packet was received
     *
     * @param packet
     *            the packet to be updated
     */
    private void updatePacketRecvAddress(DatagramPacket packet) {
        packet.setAddress(connectedAddress);
        packet.setPort(connectedPort);
    }
}
