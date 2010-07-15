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

package dalvik.system;

import org.apache.harmony.luni.platform.IFileSystem;
import org.apache.harmony.luni.platform.INetworkSystem;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.UnknownHostException;
import java.nio.channels.Channel;

/**
 * Mechanism to let threads set restrictions on what code is allowed
 * to do in their thread.
 *
 * <p>This is meant for applications to prevent certain blocking
 * operations from running on their main event loop (or "UI") threads.
 *
 * <p>Note that this is all best-effort to catch most accidental mistakes
 * and isn't intended to be a perfect mechanism, nor provide any sort of
 * security.
 *
 * @hide
 */
public final class BlockGuard {

    public static final int DISALLOW_DISK_WRITE = 0x01;
    public static final int DISALLOW_DISK_READ = 0x02;
    public static final int DISALLOW_NETWORK = 0x04;
    public static final int PASS_RESTRICTIONS_VIA_RPC = 0x08;
    public static final int PENALTY_LOG = 0x10;
    public static final int PENALTY_DIALOG = 0x20;
    public static final int PENALTY_DEATH = 0x40;

    public interface Policy {
        /**
         * Called on disk writes.
         */
        void onWriteToDisk();

        /**
         * Called on disk writes.
         */
        void onReadFromDisk();

        /**
         * Called on network operations.
         */
        void onNetwork();

        /**
         * Returns the policy bitmask, for shipping over Binder calls
         * to remote threads/processes and reinstantiating the policy
         * there.  The bits in the mask are from the DISALLOW_* and
         * PENALTY_* constants.
         */
        int getPolicyMask();
    }

    public static class BlockGuardPolicyException extends RuntimeException {
        // bitmask of DISALLOW_*, PENALTY_*, etc flags
        private final int mPolicyState;
        private final int mPolicyViolated;

        public BlockGuardPolicyException(int policyState, int policyViolated) {
            mPolicyState = policyState;
            mPolicyViolated = policyViolated;
            fillInStackTrace();
        }

        public int getPolicy() {
            return mPolicyState;
        }

        public int getPolicyViolation() {
            return mPolicyViolated;
        }

        public String getMessage() {
            // Note: do not change this format casually.  It's
            // somewhat unfortunately Parceled and passed around
            // Binder calls and parsed back into an Exception by
            // Android's StrictMode.  This was the least invasive
            // option and avoided a gross mix of Java Serialization
            // combined with Parcels.
            return "policy=" + mPolicyState + " violation=" + mPolicyViolated;
        }
    }

    /**
     * The default, permissive policy that doesn't prevent any operations.
     */
    public static Policy LAX_POLICY = new Policy() {
            public void onWriteToDisk() {}
            public void onReadFromDisk() {}
            public void onNetwork() {}
            public int getPolicyMask() {
                return 0;
            }
        };

    private static ThreadLocal<Policy> threadPolicy = new ThreadLocal<Policy>() {
        @Override protected Policy initialValue() {
            return LAX_POLICY;
        }
    };

    /**
     * Get the current thread's policy.
     *
     * @returns the current thread's policy.  Never returns null.
     *     Will return the LAX_POLICY instance if nothing else is set.
     */
    public static Policy getThreadPolicy() {
        return threadPolicy.get();
    }

    /**
     * Sets the current thread's block guard policy.
     *
     * @param policy policy to set.  May not be null.  Use the public LAX_POLICY
     *   if you want to unset the active policy.
     */
    public static void setThreadPolicy(Policy policy) {
        if (policy == null) {
            throw new NullPointerException("policy == null");
        }
        threadPolicy.set(policy);
    }

    private BlockGuard() {}

    /**
     * A filesystem wrapper that calls the policy check functions
     * on reads and writes.
     */
    public static class WrappedFileSystem implements IFileSystem {
        private final IFileSystem mFileSystem;

        public WrappedFileSystem(IFileSystem fileSystem) {
            mFileSystem = fileSystem;
        }

        public long read(int fileDescriptor, byte[] bytes, int offset, int length)
                throws IOException {
            BlockGuard.getThreadPolicy().onReadFromDisk();
            return mFileSystem.read(fileDescriptor, bytes, offset, length);
        }

        public long write(int fileDescriptor, byte[] bytes, int offset, int length)
                throws IOException {
            BlockGuard.getThreadPolicy().onWriteToDisk();
            return mFileSystem.write(fileDescriptor, bytes, offset, length);
        }

        public long readv(int fileDescriptor, int[] addresses, int[] offsets,
                          int[] lengths, int size) throws IOException {
            BlockGuard.getThreadPolicy().onReadFromDisk();
            return mFileSystem.readv(fileDescriptor, addresses, offsets, lengths, size);
        }

        public long writev(int fileDescriptor, int[] addresses, int[] offsets,
                           int[] lengths, int size) throws IOException {
            BlockGuard.getThreadPolicy().onWriteToDisk();
            return mFileSystem.writev(fileDescriptor, addresses, offsets, lengths, size);
        }

        public long readDirect(int fileDescriptor, int address, int offset,
                               int length) throws IOException {
            BlockGuard.getThreadPolicy().onReadFromDisk();
            return mFileSystem.readDirect(fileDescriptor, address, offset, length);
        }

        public long writeDirect(int fileDescriptor, int address, int offset,
                                int length) throws IOException {
            BlockGuard.getThreadPolicy().onWriteToDisk();
            return mFileSystem.writeDirect(fileDescriptor, address, offset, length);
        }

        public boolean lock(int fileDescriptor, long start, long length, int type,
                            boolean waitFlag) throws IOException {
            return mFileSystem.lock(fileDescriptor, start, length, type, waitFlag);
        }

        public void unlock(int fileDescriptor, long start, long length)
                throws IOException {
            mFileSystem.unlock(fileDescriptor, start, length);
        }

        public long seek(int fileDescriptor, long offset, int whence)
                throws IOException {
            return mFileSystem.seek(fileDescriptor, offset, whence);
        }

        public void fflush(int fileDescriptor, boolean metadata)
                throws IOException {
            BlockGuard.getThreadPolicy().onWriteToDisk();
            mFileSystem.fflush(fileDescriptor, metadata);
        }

        public void close(int fileDescriptor) throws IOException {
            mFileSystem.close(fileDescriptor);
        }

        public void truncate(int fileDescriptor, long size) throws IOException {
            BlockGuard.getThreadPolicy().onWriteToDisk();
            mFileSystem.truncate(fileDescriptor, size);
        }

        public int getAllocGranularity() throws IOException {
            return mFileSystem.getAllocGranularity();
        }

        public int open(byte[] fileName, int mode) throws FileNotFoundException {
            BlockGuard.getThreadPolicy().onReadFromDisk();
            if (mode != 0) {  // 0 is read-only
                BlockGuard.getThreadPolicy().onWriteToDisk();
            }
            return mFileSystem.open(fileName, mode);
        }

        public long transfer(int fileHandler, FileDescriptor socketDescriptor,
                             long offset, long count) throws IOException {
            return mFileSystem.transfer(fileHandler, socketDescriptor, offset, count);
        }

        public int ioctlAvailable(int fileDescriptor) throws IOException {
            return mFileSystem.ioctlAvailable(fileDescriptor);
        }
    }

    /**
     * A network wrapper that calls the policy check functions.
     */
    public static class WrappedNetworkSystem implements INetworkSystem {
        private final INetworkSystem mNetwork;

        public WrappedNetworkSystem(INetworkSystem network) {
            mNetwork = network;
        }

        public void accept(FileDescriptor fdServer, SocketImpl newSocket,
                FileDescriptor fdnewSocket, int timeout) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            mNetwork.accept(fdServer, newSocket, fdnewSocket, timeout);
        }


        public void bind(FileDescriptor aFD, InetAddress inetAddress, int port)
                throws SocketException {
            mNetwork.bind(aFD, inetAddress, port);
        }

        public int read(FileDescriptor aFD, byte[] data, int offset, int count,
                int timeout) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.read(aFD, data, offset, count, timeout);
        }

        public int readDirect(FileDescriptor aFD, int address, int count,
                int timeout) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.readDirect(aFD, address, count, timeout);
        }

        public int write(FileDescriptor fd, byte[] data, int offset, int count)
                throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.write(fd, data, offset, count);
        }

        public int writeDirect(FileDescriptor fd, int address, int offset, int count)
                throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.writeDirect(fd, address, offset, count);
        }

        public void setNonBlocking(FileDescriptor aFD, boolean block)
                throws IOException {
            mNetwork.setNonBlocking(aFD, block);
        }

        public void connect(FileDescriptor aFD, int trafficClass,
                            InetAddress inetAddress, int port) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            mNetwork.connect(aFD, trafficClass, inetAddress, port);
        }

        public int connectWithTimeout(FileDescriptor aFD, int timeout,
                int trafficClass, InetAddress hostname, int port, int step,
                byte[] context) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.connectWithTimeout(aFD, timeout, trafficClass,
                    hostname, port, step, context);
        }

        public int sendDatagram(FileDescriptor fd, byte[] data, int offset,
                int length, int port, boolean bindToDevice, int trafficClass,
                InetAddress inetAddress) throws IOException {
            // Note: no BlockGuard violation.  We permit datagrams
            // without hostname lookups.  (short, bounded amount of time)
            return mNetwork.sendDatagram(fd, data, offset, length, port, bindToDevice,
                    trafficClass, inetAddress);
        }

        public int sendDatagramDirect(FileDescriptor fd, int address, int offset,
                int length, int port, boolean bindToDevice, int trafficClass,
                InetAddress inetAddress) throws IOException {
            // Note: no BlockGuard violation.  We permit datagrams
            // without hostname lookups.  (short, bounded amount of time)
            return mNetwork.sendDatagramDirect(fd, address, offset, length, port,
                    bindToDevice, trafficClass, inetAddress);
        }

        public int receiveDatagram(FileDescriptor aFD, DatagramPacket packet,
                byte[] data, int offset, int length, int receiveTimeout,
                boolean peek) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.receiveDatagram(aFD, packet, data, offset,
                    length, receiveTimeout, peek);
        }

        public int receiveDatagramDirect(FileDescriptor aFD, DatagramPacket packet,
                int address, int offset, int length, int receiveTimeout,
                boolean peek) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.receiveDatagramDirect(aFD, packet, address, offset, length,
                    receiveTimeout, peek);
        }

        public int recvConnectedDatagram(FileDescriptor aFD, DatagramPacket packet,
                byte[] data, int offset, int length, int receiveTimeout,
                boolean peek) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.recvConnectedDatagram(aFD, packet, data, offset, length,
                    receiveTimeout, peek);
        }

        public int recvConnectedDatagramDirect(FileDescriptor aFD,
                DatagramPacket packet, int address, int offset, int length,
                int receiveTimeout, boolean peek) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.recvConnectedDatagramDirect(aFD, packet, address, offset, length,
                    receiveTimeout, peek);
        }

        public int peekDatagram(FileDescriptor aFD, InetAddress sender,
                int receiveTimeout) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.peekDatagram(aFD, sender, receiveTimeout);
        }

        public int sendConnectedDatagram(FileDescriptor fd, byte[] data,
                int offset, int length, boolean bindToDevice) throws IOException {
            return mNetwork.sendConnectedDatagram(fd, data, offset, length, bindToDevice);
        }

        public int sendConnectedDatagramDirect(FileDescriptor fd, int address,
                int offset, int length, boolean bindToDevice) throws IOException {
            return mNetwork.sendConnectedDatagramDirect(fd, address, offset, length, bindToDevice);
        }

        public void disconnectDatagram(FileDescriptor aFD) throws SocketException {
            mNetwork.disconnectDatagram(aFD);
        }

        public void createDatagramSocket(FileDescriptor aFD, boolean preferIPv4Stack)
                throws SocketException {
            mNetwork.createDatagramSocket(aFD, preferIPv4Stack);
        }

        public void connectDatagram(FileDescriptor aFD, int port, int trafficClass,
                InetAddress inetAddress) throws SocketException {
            mNetwork.connectDatagram(aFD, port, trafficClass, inetAddress);
        }

        public void shutdownInput(FileDescriptor descriptor) throws IOException {
            mNetwork.shutdownInput(descriptor);
        }

        public void shutdownOutput(FileDescriptor descriptor) throws IOException {
            mNetwork.shutdownOutput(descriptor);
        }

        public boolean supportsUrgentData(FileDescriptor fd) {
            return mNetwork.supportsUrgentData(fd);
        }

        public void sendUrgentData(FileDescriptor fd, byte value) {
            mNetwork.sendUrgentData(fd, value);
        }

        public int availableStream(FileDescriptor aFD) throws SocketException {
            return mNetwork.availableStream(aFD);
        }

        public void createServerStreamSocket(FileDescriptor aFD, boolean preferIPv4Stack)
                throws SocketException {
            mNetwork.createServerStreamSocket(aFD, preferIPv4Stack);
        }

        public void createStreamSocket(FileDescriptor aFD, boolean preferIPv4Stack)
                throws SocketException {
            mNetwork.createStreamSocket(aFD, preferIPv4Stack);
        }

        public void listenStreamSocket(FileDescriptor aFD, int backlog)
                throws SocketException {
            mNetwork.listenStreamSocket(aFD, backlog);
        }

        public void connectStreamWithTimeoutSocket(FileDescriptor aFD, int aport,
                int timeout, int trafficClass, InetAddress inetAddress)
                throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            mNetwork.connectStreamWithTimeoutSocket(aFD, aport,
                    timeout, trafficClass, inetAddress);
        }

        public int sendDatagram2(FileDescriptor fd, byte[] data, int offset,
                int length, int port, InetAddress inetAddress) throws IOException {
            return mNetwork.sendDatagram2(fd, data, offset, length, port, inetAddress);
        }

        public InetAddress getSocketLocalAddress(FileDescriptor aFD) {
            return mNetwork.getSocketLocalAddress(aFD);
        }

        public boolean select(FileDescriptor[] readFDs, FileDescriptor[] writeFDs,
                int numReadable, int numWritable, long timeout, int[] flags)
                throws SocketException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.select(readFDs, writeFDs, numReadable, numWritable, timeout, flags);
        }

        public int getSocketLocalPort(FileDescriptor aFD) {
            return mNetwork.getSocketLocalPort(aFD);
        }

        public Object getSocketOption(FileDescriptor aFD, int opt)
                throws SocketException {
            return mNetwork.getSocketOption(aFD, opt);
        }

        public void setSocketOption(FileDescriptor aFD, int opt, Object optVal)
                throws SocketException {
            mNetwork.setSocketOption(aFD, opt, optVal);
        }

        public int getSocketFlags() {
            return mNetwork.getSocketFlags();
        }

        public void socketClose(FileDescriptor aFD) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            mNetwork.socketClose(aFD);
        }

        public InetAddress getHostByAddr(byte[] addr) throws UnknownHostException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.getHostByAddr(addr);
        }

        public InetAddress getHostByName(String addr) throws UnknownHostException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.getHostByName(addr);
        }

        public void setInetAddress(InetAddress sender, byte[] address) {
            mNetwork.setInetAddress(sender, address);
        }

        public String byteArrayToIpString(byte[] address)
                throws UnknownHostException {
            return mNetwork.byteArrayToIpString(address);
        }

        public byte[] ipStringToByteArray(String address)
                throws UnknownHostException {
            return mNetwork.ipStringToByteArray(address);
        }

        public Channel inheritedChannel() {
            return mNetwork.inheritedChannel();
        }
    }
}
