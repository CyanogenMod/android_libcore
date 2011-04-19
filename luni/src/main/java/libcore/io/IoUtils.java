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

package libcore.io;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOptions;
import java.util.Arrays;
import libcore.io.ErrnoException;
import libcore.io.Libcore;
import libcore.util.MutableInt;
import static libcore.io.OsConstants.*;

public final class IoUtils {
    private IoUtils() {
    }

    /**
     * Implements java.io/java.net "available" semantics.
     */
    public static int available(FileDescriptor fd) throws IOException {
        try {
            MutableInt available = new MutableInt(0);
            int rc = Libcore.os.ioctlInt(fd, FIONREAD, available);
            if (available.value < 0) {
                // If the fd refers to a regular file, the result is the difference between
                // the file size and the file position. This may be negative if the position
                // is past the end of the file. If the fd refers to a special file masquerading
                // as a regular file, the result may be negative because the special file
                // may appear to have zero size and yet a previous read call may have
                // read some amount of data and caused the file position to be advanced.
                available.value = 0;
            }
            return available.value;
        } catch (ErrnoException errnoException) {
            if (errnoException.errno == ENOTTY) {
                // The fd is unwilling to opine about its read buffer.
                return 0;
            }
            throw errnoException.rethrowAsIOException();
        }
    }

    /**
     * java.io only throws FileNotFoundException when opening files, regardless of what actually
     * went wrong. Additionally, java.io is more restrictive than POSIX when it comes to opening
     * directories: POSIX says read-only is okay, but java.io doesn't even allow that. We also
     * have an Android-specific hack to alter the default permissions.
     */
    public static FileDescriptor open(String path, int flags) throws FileNotFoundException {
        FileDescriptor fd = null;
        try {
            // On Android, we don't want default permissions to allow global access.
            int mode = ((flags & O_ACCMODE) == O_RDONLY) ? 0 : 0600;
            fd = Libcore.os.open(path, flags, mode);
            if (fd.valid()) {
                // Posix open(2) fails with EISDIR only if you ask for write permission.
                // Java disallows reading directories too.
                boolean isDirectory = false;
                if (S_ISDIR(Libcore.os.fstat(fd).st_mode)) {
                    throw new ErrnoException("open", EISDIR);
                }
            }
            return fd;
        } catch (ErrnoException errnoException) {
            try {
                if (fd != null) {
                    close(fd);
                }
            } catch (IOException ignored) {
            }
            FileNotFoundException ex = new FileNotFoundException(path + ": " + errnoException.getMessage());
            ex.initCause(errnoException);
            throw ex;
        }
    }

    /**
     * java.io thinks that a read at EOF is an error and should return -1, contrary to traditional
     * Unix practice where you'd read until you got 0 bytes (and any future read would return -1).
     */
    public static int read(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
        Arrays.checkOffsetAndCount(bytes.length, byteOffset, byteCount);
        if (byteCount == 0) {
            return 0;
        }
        try {
            int readCount = Libcore.os.read(fd, bytes, byteOffset, byteCount);
            if (readCount == 0) {
                return -1;
            }
            return readCount;
        } catch (ErrnoException errnoException) {
            if (errnoException.errno == EAGAIN) {
                // We return 0 rather than throw if we try to read from an empty non-blocking pipe.
                return 0;
            }
            throw errnoException.rethrowAsIOException();
        }
    }

    /**
     * java.io always writes every byte it's asked to, or fails with an error. (That is, unlike
     * Unix it never just writes as many bytes as happens to be convenient.)
     */
    public static void write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
        Arrays.checkOffsetAndCount(bytes.length, byteOffset, byteCount);
        if (byteCount == 0) {
            return;
        }
        try {
            while (byteCount > 0) {
                int bytesWritten = Libcore.os.write(fd, bytes, byteOffset, byteCount);
                byteCount -= bytesWritten;
                byteOffset += bytesWritten;
            }
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsIOException();
        }
    }

    /**
     * Calls close(2) on 'fd'. Also resets the internal int to -1. Does nothing if 'fd' is null
     * or invalid.
     */
    public static void close(FileDescriptor fd) throws IOException {
        try {
            if (fd != null && fd.valid()) {
                Libcore.os.close(fd);
            }
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsIOException();
        }
    }

    /**
     * Closes 'closeable', ignoring any exceptions. Does nothing if 'closeable' is null.
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Closes 'fd', ignoring any exceptions. Does nothing if 'fd' is null or invalid.
     */
    public static void closeQuietly(FileDescriptor fd) {
        try {
            IoUtils.close(fd);
        } catch (IOException ignored) {
        }
    }

    /**
     * Closes 'socket', ignoring any exceptions. Does nothing if 'socket' is null.
     */
    public static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Sets 'fd' to be blocking or non-blocking, according to the state of 'blocking'.
     */
    public static void setBlocking(FileDescriptor fd, boolean blocking) throws IOException {
        try {
            int flags = Libcore.os.fcntlVoid(fd, F_GETFL);
            if (!blocking) {
                flags |= O_NONBLOCK;
            } else {
                flags &= ~O_NONBLOCK;
            }
            Libcore.os.fcntlLong(fd, F_SETFL, flags);
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsIOException();
        }
    }

    public static FileDescriptor socket(boolean stream) throws SocketException {
        FileDescriptor fd;
        try {
            fd = Libcore.os.socket(AF_INET6, stream ? SOCK_STREAM : SOCK_DGRAM, 0);

            // The RFC (http://www.ietf.org/rfc/rfc3493.txt) says that IPV6_MULTICAST_HOPS defaults
            // to 1. The Linux kernel (at least up to 2.6.38) accidentally defaults to 64 (which
            // would be correct for the *unicast* hop limit).
            // See http://www.spinics.net/lists/netdev/msg129022.html, though no patch appears to
            // have been applied as a result of that discussion. If that bug is ever fixed, we can
            // remove this code. Until then, we manually set the hop limit on IPv6 datagram sockets.
            // (IPv4 is already correct.)
            if (!stream) {
                Libcore.os.setsockoptInt(fd, IPPROTO_IPV6, IPV6_MULTICAST_HOPS, 1);
            }

            return fd;
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsSocketException();
        }
    }

    // Socket options used by java.net but not exposed in SocketOptions.
    public static final int JAVA_MCAST_JOIN_GROUP = 19;
    public static final int JAVA_MCAST_LEAVE_GROUP = 20;
    public static final int JAVA_IP_MULTICAST_TTL = 17;

    /**
     * java.net has its own socket options similar to the underlying Unix ones. We paper over the
     * differences here.
     */
    public static Object getSocketOption(FileDescriptor fd, int option) throws SocketException {
        try {
            return getSocketOptionErrno(fd, option);
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsSocketException();
        }
    }

    private static Object getSocketOptionErrno(FileDescriptor fd, int option) throws SocketException {
        switch (option) {
        case SocketOptions.IP_MULTICAST_IF:
            // This is IPv4-only.
            return Libcore.os.getsockoptInAddr(fd, IPPROTO_IP, IP_MULTICAST_IF);
        case SocketOptions.IP_MULTICAST_IF2:
            // This is IPv6-only.
            return Libcore.os.getsockoptInt(fd, IPPROTO_IPV6, IPV6_MULTICAST_IF);
        case SocketOptions.IP_MULTICAST_LOOP:
            // Since setting this from java.net always sets IPv4 and IPv6 to the same value,
            // it doesn't matter which we return.
            return booleanFromInt(Libcore.os.getsockoptInt(fd, IPPROTO_IPV6, IPV6_MULTICAST_LOOP));
        case IoUtils.JAVA_IP_MULTICAST_TTL:
            // Since setting this from java.net always sets IPv4 and IPv6 to the same value,
            // it doesn't matter which we return.
            return Libcore.os.getsockoptInt(fd, IPPROTO_IPV6, IPV6_MULTICAST_HOPS);
        case SocketOptions.IP_TOS:
            // Since setting this from java.net always sets IPv4 and IPv6 to the same value,
            // it doesn't matter which we return.
            return Libcore.os.getsockoptInt(fd, IPPROTO_IPV6, IPV6_TCLASS);
        case SocketOptions.SO_BROADCAST:
            return booleanFromInt(Libcore.os.getsockoptInt(fd, SOL_SOCKET, SO_BROADCAST));
        case SocketOptions.SO_KEEPALIVE:
            return booleanFromInt(Libcore.os.getsockoptInt(fd, SOL_SOCKET, SO_KEEPALIVE));
        case SocketOptions.SO_LINGER:
            StructLinger linger = Libcore.os.getsockoptLinger(fd, SOL_SOCKET, SO_LINGER);
            if (!linger.isOn()) {
                return false;
            }
            return linger.l_linger;
        case SocketOptions.SO_OOBINLINE:
            return booleanFromInt(Libcore.os.getsockoptInt(fd, SOL_SOCKET, SO_OOBINLINE));
        case SocketOptions.SO_RCVBUF:
            return Libcore.os.getsockoptInt(fd, SOL_SOCKET, SO_SNDBUF);
        case SocketOptions.SO_REUSEADDR:
            return booleanFromInt(Libcore.os.getsockoptInt(fd, SOL_SOCKET, SO_REUSEADDR));
        case SocketOptions.SO_SNDBUF:
            return Libcore.os.getsockoptInt(fd, SOL_SOCKET, SO_SNDBUF);
        case SocketOptions.SO_TIMEOUT:
            return (int) Libcore.os.getsockoptTimeval(fd, SOL_SOCKET, SO_RCVTIMEO).toMillis();
        case SocketOptions.TCP_NODELAY:
            return booleanFromInt(Libcore.os.getsockoptInt(fd, IPPROTO_TCP, TCP_NODELAY));
        default:
            throw new SocketException("Unknown socket option: " + option);
        }
    }

    private static boolean booleanFromInt(int i) {
        return (i != 0);
    }

    private static int booleanToInt(boolean b) {
        return b ? 1 : 0;
    }

    /**
     * java.net has its own socket options similar to the underlying Unix ones. We paper over the
     * differences here.
     */
    public static void setSocketOption(FileDescriptor fd, int option, Object value) throws SocketException {
        try {
            setSocketOptionErrno(fd, option, value);
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsSocketException();
        }
    }

    private static void setSocketOptionErrno(FileDescriptor fd, int option, Object value) throws SocketException {
        switch (option) {
        case SocketOptions.IP_MULTICAST_IF:
            throw new UnsupportedOperationException("Use IP_MULTICAST_IF2 on Android");
        case SocketOptions.IP_MULTICAST_IF2:
            // Although IPv6 was cleaned up to use int, IPv4 uses an ip_mreqn containing an int.
            Libcore.os.setsockoptIpMreqn(fd, IPPROTO_IP, IP_MULTICAST_IF, (Integer) value);
            Libcore.os.setsockoptInt(fd, IPPROTO_IPV6, IPV6_MULTICAST_IF, (Integer) value);
            return;
        case SocketOptions.IP_MULTICAST_LOOP:
            // Although IPv6 was cleaned up to use int, IPv4 multicast loopback uses a byte.
            Libcore.os.setsockoptByte(fd, IPPROTO_IP, IP_MULTICAST_LOOP, booleanToInt((Boolean) value));
            Libcore.os.setsockoptInt(fd, IPPROTO_IPV6, IPV6_MULTICAST_LOOP, booleanToInt((Boolean) value));
            return;
        case IoUtils.JAVA_IP_MULTICAST_TTL:
            // Although IPv6 was cleaned up to use int, and IPv4 non-multicast TTL uses int,
            // IPv4 multicast TTL uses a byte.
            Libcore.os.setsockoptByte(fd, IPPROTO_IP, IP_MULTICAST_TTL, (Integer) value);
            Libcore.os.setsockoptInt(fd, IPPROTO_IPV6, IPV6_MULTICAST_HOPS, (Integer) value);
            return;
        case SocketOptions.IP_TOS:
            Libcore.os.setsockoptInt(fd, IPPROTO_IP, IP_TOS, (Integer) value);
            Libcore.os.setsockoptInt(fd, IPPROTO_IPV6, IPV6_TCLASS, (Integer) value);
            return;
        case SocketOptions.SO_BROADCAST:
            Libcore.os.setsockoptInt(fd, SOL_SOCKET, SO_BROADCAST, booleanToInt((Boolean) value));
            return;
        case SocketOptions.SO_KEEPALIVE:
            Libcore.os.setsockoptInt(fd, SOL_SOCKET, SO_KEEPALIVE, booleanToInt((Boolean) value));
            return;
        case SocketOptions.SO_LINGER:
            boolean on = false;
            int seconds = 0;
            if (value instanceof Integer) {
                on = true;
                seconds = Math.min((Integer) value, 65535);
            }
            StructLinger linger = new StructLinger(booleanToInt(on), seconds);
            Libcore.os.setsockoptLinger(fd, SOL_SOCKET, SO_LINGER, linger);
            return;
        case SocketOptions.SO_OOBINLINE:
            Libcore.os.setsockoptInt(fd, SOL_SOCKET, SO_OOBINLINE, booleanToInt((Boolean) value));
            return;
        case SocketOptions.SO_RCVBUF:
            Libcore.os.setsockoptInt(fd, SOL_SOCKET, SO_RCVBUF, (Integer) value);
            return;
        case SocketOptions.SO_REUSEADDR:
            Libcore.os.setsockoptInt(fd, SOL_SOCKET, SO_REUSEADDR, booleanToInt((Boolean) value));
            return;
        case SocketOptions.SO_SNDBUF:
            Libcore.os.setsockoptInt(fd, SOL_SOCKET, SO_SNDBUF, (Integer) value);
            return;
        case SocketOptions.SO_TIMEOUT:
            int millis = (Integer) value;
            StructTimeval tv = StructTimeval.fromMillis(millis);
            Libcore.os.setsockoptTimeval(fd, SOL_SOCKET, SO_RCVTIMEO, tv);
            return;
        case SocketOptions.TCP_NODELAY:
            Libcore.os.setsockoptInt(fd, IPPROTO_TCP, TCP_NODELAY, booleanToInt((Boolean) value));
            return;
        case IoUtils.JAVA_MCAST_JOIN_GROUP:
        case IoUtils.JAVA_MCAST_LEAVE_GROUP:
            StructGroupReq groupReq = (StructGroupReq) value;
            int level = (groupReq.gr_group instanceof Inet4Address) ? IPPROTO_IP : IPPROTO_IPV6;
            int op = (option == JAVA_MCAST_JOIN_GROUP) ? MCAST_JOIN_GROUP : MCAST_LEAVE_GROUP;
            Libcore.os.setsockoptGroupReq(fd, level, op, groupReq);
            return;
        default:
            throw new SocketException("Unknown socket option: " + option);
        }
    }

    public static InetAddress getSocketLocalAddress(FileDescriptor fd) {
        SocketAddress sa = Libcore.os.getsockname(fd);
        InetSocketAddress isa = (InetSocketAddress) sa;
        return isa.getAddress();
    }

    public static int getSocketLocalPort(FileDescriptor fd) {
        SocketAddress sa = Libcore.os.getsockname(fd);
        InetSocketAddress isa = (InetSocketAddress) sa;
        return isa.getPort();
    }

    /**
     * Returns the contents of 'path' as a byte array.
     */
    public static byte[] readFileAsByteArray(String path) throws IOException {
        RandomAccessFile f = null;
        try {
            f = new RandomAccessFile(path, "r");
            byte[] buf = new byte[(int) f.length()];
            f.readFully(buf);
            return buf;
        } finally {
            IoUtils.closeQuietly(f);
        }
    }

    public static boolean preferIPv6Addresses() {
        String propertyValue = System.getProperty("java.net.preferIPv6Addresses");
        return Boolean.parseBoolean(propertyValue);
    }
}
