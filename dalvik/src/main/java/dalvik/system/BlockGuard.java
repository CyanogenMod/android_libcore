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

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketOptions;
import libcore.io.Libcore;
import libcore.io.StructLinger;
import org.apache.harmony.luni.platform.INetworkSystem;
import static libcore.io.OsConstants.*;

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
        private final String mMessage;   // may be null

        public BlockGuardPolicyException(int policyState, int policyViolated) {
            this(policyState, policyViolated, null);
        }

        public BlockGuardPolicyException(int policyState, int policyViolated, String message) {
            mPolicyState = policyState;
            mPolicyViolated = policyViolated;
            mMessage = message;
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
            return "policy=" + mPolicyState + " violation=" + mPolicyViolated +
                    (mMessage == null ? "" : (" msg=" + mMessage));
        }
    }

    /**
     * The default, permissive policy that doesn't prevent any operations.
     */
    public static final Policy LAX_POLICY = new Policy() {
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
     * @return the current thread's policy.  Never returns null.
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
     * A network wrapper that calls the policy check functions.
     */
    public static class WrappedNetworkSystem implements INetworkSystem {
        private final INetworkSystem mNetwork;

        public WrappedNetworkSystem(INetworkSystem network) {
            mNetwork = network;
        }

        public void accept(FileDescriptor serverFd, SocketImpl newSocket,
                FileDescriptor clientFd) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            mNetwork.accept(serverFd, newSocket, clientFd);
        }

        public int read(FileDescriptor aFD, byte[] data, int offset, int count) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.read(aFD, data, offset, count);
        }

        public int readDirect(FileDescriptor aFD, int address, int count) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.readDirect(aFD, address, count);
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

        public boolean isConnected(FileDescriptor fd, int timeout) throws IOException {
            if (timeout != 0) {
                // Greater than 0 is a timeout, but zero means "poll and return immediately".
                BlockGuard.getThreadPolicy().onNetwork();
            }
            return mNetwork.isConnected(fd, timeout);
        }

        public int send(FileDescriptor fd, byte[] data, int offset, int length,
                int port, InetAddress inetAddress) throws IOException {
            // Note: no BlockGuard violation.  We permit datagrams
            // without hostname lookups.  (short, bounded amount of time)
            return mNetwork.send(fd, data, offset, length, port, inetAddress);
        }

        public int sendDirect(FileDescriptor fd, int address, int offset, int length,
                int port, InetAddress inetAddress) throws IOException {
            // Note: no BlockGuard violation.  We permit datagrams
            // without hostname lookups.  (short, bounded amount of time)
            return mNetwork.sendDirect(fd, address, offset, length, port, inetAddress);
        }

        public int recv(FileDescriptor fd, DatagramPacket packet, byte[] data, int offset,
                int length, boolean peek, boolean connected) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.recv(fd, packet, data, offset, length, peek, connected);
        }

        public int recvDirect(FileDescriptor fd, DatagramPacket packet, int address, int offset,
                int length, boolean peek, boolean connected) throws IOException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.recvDirect(fd, packet, address, offset, length, peek, connected);
        }

        public void disconnectDatagram(FileDescriptor aFD) throws SocketException {
            mNetwork.disconnectDatagram(aFD);
        }

        public void sendUrgentData(FileDescriptor fd, byte value) {
            mNetwork.sendUrgentData(fd, value);
        }

        public boolean select(FileDescriptor[] readFDs, FileDescriptor[] writeFDs,
                int numReadable, int numWritable, long timeout, int[] flags)
                throws SocketException {
            BlockGuard.getThreadPolicy().onNetwork();
            return mNetwork.select(readFDs, writeFDs, numReadable, numWritable, timeout, flags);
        }

        public void close(FileDescriptor aFD) throws IOException {
            // We exclude sockets without SO_LINGER so that apps can close their network connections
            // in methods like onDestroy, which will run on the UI thread, without jumping through
            // extra hoops.
            if (isLingerSocket(aFD)) {
                BlockGuard.getThreadPolicy().onNetwork();
            }
            mNetwork.close(aFD);
        }

        private boolean isLingerSocket(FileDescriptor fd) throws SocketException {
            try {
                StructLinger linger = Libcore.os.getsockoptLinger(fd, SOL_SOCKET, SO_LINGER);
                return linger.isOn() && linger.l_linger > 0;
            } catch (Exception ignored) {
                // We're called via Socket.close (which doesn't ask for us to be called), so we
                // must not throw here, because Socket.close must not throw if asked to close an
                // already-closed socket.
                return false;
            }
        }
    }
}
