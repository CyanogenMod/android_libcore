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

package org.apache.harmony.luni.platform;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketImpl;

/*
 * The interface for network methods.
 */
public interface INetworkSystem {
    public void accept(FileDescriptor serverFd, SocketImpl newSocket, FileDescriptor clientFd) throws IOException;

    public void close(FileDescriptor fd) throws IOException;

    public boolean isConnected(FileDescriptor fd, int timeout) throws IOException;

    public int read(FileDescriptor fd, byte[] data, int offset, int count) throws IOException;
    public int readDirect(FileDescriptor fd, int address, int count) throws IOException;
    public int recv(FileDescriptor fd, DatagramPacket packet, byte[] data, int offset, int length, boolean peek, boolean connected) throws IOException;
    public int recvDirect(FileDescriptor fd, DatagramPacket packet, int address, int offset, int length, boolean peek, boolean connected) throws IOException;

    public int select(FileDescriptor[] readFDs, FileDescriptor[] writeFDs, long timeout, int[] flags) throws SocketException;

    public int send(FileDescriptor fd, byte[] data, int offset, int length, int port, InetAddress inetAddress) throws IOException;
    public int sendDirect(FileDescriptor fd, int address, int offset, int length, int port, InetAddress inetAddress) throws IOException;
    public void sendUrgentData(FileDescriptor fd, byte value);
    public int write(FileDescriptor fd, byte[] data, int offset, int count) throws IOException;
    public int writeDirect(FileDescriptor fd, int address, int offset, int count) throws IOException;
}
