/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "OSNetworkSystem"

#include "JNIHelp.h"
#include "JniConstants.h"
#include "JniException.h"
#include "LocalArray.h"
#include "NetworkUtilities.h"
#include "ScopedPrimitiveArray.h"
#include "jni.h"
#include "valueOf.h"

#include <arpa/inet.h>
#include <assert.h>
#include <errno.h>
#include <netdb.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/un.h>
#include <unistd.h>

// Temporary hack to build on systems that don't have up-to-date libc headers.
#ifndef IPV6_TCLASS
#ifdef __linux__
#define IPV6_TCLASS 67 // Linux
#else
#define IPV6_TCLASS -1 // BSD(-like); TODO: Something better than this!
#endif
#endif

/*
 * TODO: The multicast code is highly platform-dependent, and for now
 * we just punt on anything but Linux.
 */
#ifdef __linux__
#define ENABLE_MULTICAST
#endif

#define JAVASOCKOPT_IP_MULTICAST_IF 16
#define JAVASOCKOPT_IP_MULTICAST_IF2 31
#define JAVASOCKOPT_IP_MULTICAST_LOOP 18
#define JAVASOCKOPT_IP_TOS 3
#define JAVASOCKOPT_MCAST_ADD_MEMBERSHIP 19
#define JAVASOCKOPT_MCAST_DROP_MEMBERSHIP 20
#define JAVASOCKOPT_MULTICAST_TTL 17
#define JAVASOCKOPT_SO_BROADCAST 32
#define JAVASOCKOPT_SO_KEEPALIVE 8
#define JAVASOCKOPT_SO_LINGER 128
#define JAVASOCKOPT_SO_OOBINLINE  4099
#define JAVASOCKOPT_SO_RCVBUF 4098
#define JAVASOCKOPT_SO_TIMEOUT  4102
#define JAVASOCKOPT_SO_REUSEADDR 4
#define JAVASOCKOPT_SO_SNDBUF 4097
#define JAVASOCKOPT_TCP_NODELAY 1

/* constants for calling multi-call functions */
#define SOCKET_STEP_START 10
#define SOCKET_STEP_CHECK 20
#define SOCKET_STEP_DONE 30

#define SOCKET_CONNECT_STEP_START 0
#define SOCKET_CONNECT_STEP_CHECK 1

#define SOCKET_OP_NONE 0
#define SOCKET_OP_READ 1
#define SOCKET_OP_WRITE 2

static struct CachedFields {
    jfieldID iaddr_ipaddress;
    jfieldID integer_class_value;
    jfieldID boolean_class_value;
    jfieldID socketimpl_address;
    jfieldID socketimpl_port;
    jfieldID socketimpl_localport;
    jfieldID dpack_address;
    jfieldID dpack_port;
    jfieldID dpack_length;
} gCachedFields;

/* needed for connecting with timeout */
struct selectFDSet {
  int nfds;
  int sock;
  fd_set writeSet;
  fd_set readSet;
  fd_set exceptionSet;
};

/**
 * Wraps access to the int inside a java.io.FileDescriptor, taking care of throwing exceptions.
 */
class NetFd {
public:
    NetFd(JNIEnv* env, jobject fileDescriptor)
        : mEnv(env), mFileDescriptor(fileDescriptor), mFd(-1)
    {
    }

    bool isClosed() {
        mFd = jniGetFDFromFileDescriptor(mEnv, mFileDescriptor);
        bool closed = (mFd == -1);
        if (closed) {
            jniThrowException(mEnv, "java/net/SocketException", "Socket closed");
        }
        return closed;
    }

    int get() const {
        return mFd;
    }

private:
    JNIEnv* mEnv;
    jobject mFileDescriptor;
    int mFd;

    // Disallow copy and assignment.
    NetFd(const NetFd&);
    void operator=(const NetFd&);
};

/**
 * Used to retry syscalls that can return EINTR. This differs from TEMP_FAILURE_RETRY in that
 * it also considers the case where the reason for failure is that another thread called
 * shutdown(2) on the socket.
 */
#define NET_FAILURE_RETRY(env, fd, exp) ({                          \
    typeof (exp) _rc;                                               \
    do {                                                            \
        _rc = (exp);                                                \
        if (_rc == -1) {                                            \
            if (fd.isClosed()) {                                    \
                break;                                              \
            }                                                       \
            if (errno != EINTR) {                                   \
                if (errno == EAGAIN || errno == EWOULDBLOCK) {      \
                    jniThrowSocketTimeoutException(env, ETIMEDOUT); \
                } else {                                            \
                    jniThrowSocketException(env, errno);            \
                }                                                   \
            }                                                       \
        }                                                           \
    } while (_rc == -1 && errno == EINTR);                          \
    _rc; })

/**
 * Returns the port number in a sockaddr_storage structure.
 *
 * @param address the sockaddr_storage structure to get the port from
 *
 * @return the port number, or -1 if the address family is unknown.
 */
static int getSocketAddressPort(struct sockaddr_storage *address) {
    switch (address->ss_family) {
        case AF_INET:
            return ntohs(((struct sockaddr_in *) address)->sin_port);
        case AF_INET6:
            return ntohs(((struct sockaddr_in6 *) address)->sin6_port);
        default:
            return -1;
    }
}

/**
 * Obtain the socket address family from an existing socket.
 *
 * @param socket the file descriptor of the socket to examine
 * @return an integer, the address family of the socket
 */
static int getSocketAddressFamily(int socket) {
    sockaddr_storage ss;
    socklen_t namelen = sizeof(ss);
    int ret = getsockname(socket, (sockaddr*) &ss, &namelen);
    if (ret != 0) {
        return AF_UNSPEC;
    } else {
        return ss.ss_family;
    }
}

// Handles translating between IPv4 and IPv6 addresses so -- where possible --
// we can use either class of address with either an IPv4 or IPv6 socket.
class CompatibleSocketAddress {
public:
    // Constructs an address corresponding to 'ss' that's compatible with 'fd'.
    CompatibleSocketAddress(int fd, const sockaddr_storage& ss, bool mapUnspecified) {
        const int desiredFamily = getSocketAddressFamily(fd);
        if (ss.ss_family == AF_INET6) {
            if (desiredFamily == AF_INET6) {
                // Nothing to do.
                mCompatibleAddress = reinterpret_cast<const sockaddr*>(&ss);
            } else {
                sockaddr_in* sin = reinterpret_cast<sockaddr_in*>(&mTmp);
                const sockaddr_in6* sin6 = reinterpret_cast<const sockaddr_in6*>(&ss);
                memset(sin, 0, sizeof(*sin));
                sin->sin_family = AF_INET;
                sin->sin_port = sin6->sin6_port;
                if (IN6_IS_ADDR_V4COMPAT(&sin6->sin6_addr)) {
                    // We have an IPv6-mapped IPv4 address, but need plain old IPv4.
                    // Unmap the mapped address in ss into an IPv6 address in mTmp.
                    memcpy(&sin->sin_addr.s_addr, &sin6->sin6_addr.s6_addr[12], 4);
                    mCompatibleAddress = reinterpret_cast<const sockaddr*>(&mTmp);
                } else if (IN6_IS_ADDR_LOOPBACK(&sin6->sin6_addr)) {
                    // Translate the IPv6 loopback address to the IPv4 one.
                    sin->sin_addr.s_addr = htonl(INADDR_LOOPBACK);
                    mCompatibleAddress = reinterpret_cast<const sockaddr*>(&mTmp);
                } else {
                    // We can't help you. We return what you gave us, and assume you'll
                    // get a sensible error when you use the address.
                    mCompatibleAddress = reinterpret_cast<const sockaddr*>(&ss);
                }
            }
        } else /* ss.ss_family == AF_INET */ {
            if (desiredFamily == AF_INET) {
                // Nothing to do.
                mCompatibleAddress = reinterpret_cast<const sockaddr*>(&ss);
            } else {
                // We have IPv4 and need IPv6.
                // Map the IPv4 address in ss into an IPv6 address in mTmp.
                const sockaddr_in* sin = reinterpret_cast<const sockaddr_in*>(&ss);
                sockaddr_in6* sin6 = reinterpret_cast<sockaddr_in6*>(&mTmp);
                memset(sin6, 0, sizeof(*sin6));
                sin6->sin6_family = AF_INET6;
                sin6->sin6_port = sin->sin_port;
                // TODO: mapUnspecified was introduced because kernels < 2.6.31 don't allow
                // you to bind to ::ffff:0.0.0.0. When we move to something >= 2.6.31, we
                // should make the code behave as if mapUnspecified were always true, and
                // remove the parameter.
                if (sin->sin_addr.s_addr != 0 || mapUnspecified) {
                    memset(&(sin6->sin6_addr.s6_addr[10]), 0xff, 2);
                }
                memcpy(&sin6->sin6_addr.s6_addr[12], &sin->sin_addr.s_addr, 4);
                mCompatibleAddress = reinterpret_cast<const sockaddr*>(&mTmp);
            }
        }
    }
    // Returns a pointer to an address compatible with the socket.
    const sockaddr* get() const {
        return mCompatibleAddress;
    }
private:
    const sockaddr* mCompatibleAddress;
    sockaddr_storage mTmp;
};

/**
 * Converts an InetAddress object and port number to a native address structure.
 */
static bool inetAddressToSocketAddress(JNIEnv *env, jobject inetaddress,
        int port, sockaddr_storage *sockaddress) {
    // Get the byte array that stores the IP address bytes in the InetAddress.
    if (inetaddress == NULL) {
        jniThrowNullPointerException(env, NULL);
        return false;
    }
    jbyteArray addressBytes =
        reinterpret_cast<jbyteArray>(env->GetObjectField(inetaddress,
            gCachedFields.iaddr_ipaddress));

    return byteArrayToSocketAddress(env, NULL, addressBytes, port, sockaddress);
}

// Converts a number of milliseconds to a timeval.
static timeval toTimeval(long ms) {
    timeval tv;
    tv.tv_sec = ms / 1000;
    tv.tv_usec = (ms - tv.tv_sec*1000) * 1000;
    return tv;
}

// Converts a timeval to a number of milliseconds.
static long toMs(const timeval& tv) {
    return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

/**
 * Query OS for timestamp.
 * Retrieve the current value of system clock and convert to milliseconds.
 *
 * @param[in] portLibrary The port library.
 *
 * @return 0 on failure, time value in milliseconds on success.
 * @deprecated Use @ref time_hires_clock and @ref time_hires_delta
 *
 * technically, this should return uint64_t since both timeval.tv_sec and
 * timeval.tv_usec are long
 */
static int time_msec_clock() {
    timeval tp;
    struct timezone tzp;
    gettimeofday(&tp, &tzp);
    return toMs(tp);
}

/**
 * Wrapper for connect() that converts IPv4 addresses to IPv4-mapped IPv6
 * addresses if necessary.
 *
 * @param socket the file descriptor of the socket to connect
 * @param socketAddress the address to connect to
 */
static int doConnect(int fd, const sockaddr_storage* socketAddress) {
    const CompatibleSocketAddress compatibleAddress(fd, *socketAddress, true);
    return TEMP_FAILURE_RETRY(connect(fd, compatibleAddress.get(), sizeof(sockaddr_storage)));
}

/**
 * Establish a connection to a peer with a timeout.  This function is called
 * repeatedly in order to carry out the connect and to allow other tasks to
 * proceed on certain platforms. The caller must first call with
 * step = SOCKET_STEP_START, if the result is -EINPROGRESS it will then
 * call it with step = CHECK until either another error or 0 is returned to
 * indicate the connect is complete.  Each time the function should sleep for no
 * more than timeout milliseconds.  If the connect succeeds or an error occurs,
 * the caller must always end the process by calling the function with
 * step = SOCKET_STEP_DONE
 *
 * @param[in] timeout the timeout in milliseconds. If timeout is negative,
 *         perform a block operation.
 *
 * @return 0, if no errors occurred, otherwise -errno. TODO: use +errno.
 */
static int sockConnectWithTimeout(int fd, const sockaddr_storage& addr, int timeout, unsigned int step, selectFDSet* context) {
    int errorVal;
    socklen_t errorValLen = sizeof(int);

    if (step == SOCKET_STEP_START) {
        context->sock = fd;
        context->nfds = fd + 1;

        /* set the socket to non-blocking */
        if (!setBlocking(fd, false)) {
            return -errno;
        }

        if (doConnect(fd, &addr) == -1) {
            return -errno;
        }

        /* we connected right off the bat so just return */
        return 0;
    } else if (step == SOCKET_STEP_CHECK) {
        /* now check if we have connected yet */

        /*
         * set the timeout value to be used. Because on some unix platforms we
         * don't get notified when a socket is closed we only sleep for 100ms
         * at a time
         *
         * TODO: is this relevant for Android?
         */
        if (timeout > 100) {
            timeout = 100;
        }
        timeval passedTimeout(toTimeval(timeout));

        /* initialize the FD sets for the select */
        FD_ZERO(&(context->exceptionSet));
        FD_ZERO(&(context->writeSet));
        FD_ZERO(&(context->readSet));
        FD_SET(context->sock, &(context->writeSet));
        FD_SET(context->sock, &(context->readSet));
        FD_SET(context->sock, &(context->exceptionSet));

        int rc = TEMP_FAILURE_RETRY(select(context->nfds,
                &(context->readSet), &(context->writeSet), &(context->exceptionSet),
                timeout >= 0 ? &passedTimeout : NULL));

        /* if there is at least one descriptor ready to be checked */
        if (rc > 0) {
            /* if the descriptor is in the write set we connected or failed */
            if (FD_ISSET(context->sock, &(context->writeSet))) {
                if (!FD_ISSET(context->sock, &(context->readSet))) {
                    /* ok we have connected ok */
                    return 0;
                } else {
                    /* ok we have more work to do to figure it out */
                    if (getsockopt(context->sock, SOL_SOCKET, SO_ERROR, &errorVal, &errorValLen) >= 0) {
                        return errorVal ? -errorVal : 0;
                    } else {
                        return -errno;
                    }
                }
            }

            /* if the descriptor is in the exception set the connect failed */
            if (FD_ISSET(context->sock, &(context->exceptionSet))) {
                if (getsockopt(context->sock, SOL_SOCKET, SO_ERROR, &errorVal, &errorValLen) >= 0) {
                    return errorVal ? -errorVal : 0;
                }
                return -errno;
            }
        } else if (rc < 0) {
            /* some other error occurred */
            return -errno;
        }

        /*
         * if we get here the timeout expired or the connect had not yet
         * completed just indicate that the connect is not yet complete
         */
        return -EINPROGRESS;
    } else if (step == SOCKET_STEP_DONE) {
        /* we are done the connect or an error occurred so clean up  */
        if (fd != -1) {
            setBlocking(fd, true);
        }
        return 0;
    }
    return -EFAULT;
}

#ifdef ENABLE_MULTICAST
/*
 * Find the interface index that was set for this socket by the IP_MULTICAST_IF
 * or IPV6_MULTICAST_IF socket option.
 *
 * @param socket the socket to examine
 *
 * @return the interface index, or -1 on failure
 *
 * @note on internal failure, the errno variable will be set appropriately
 */
static int interfaceIndexFromMulticastSocket(int socket) {
    int family = getSocketAddressFamily(socket);
    if (family == AF_INET) {
        // IP_MULTICAST_IF returns a pointer to a struct ip_mreqn.
        struct ip_mreqn tempRequest;
        socklen_t requestLength = sizeof(tempRequest);
        int rc = getsockopt(socket, IPPROTO_IP, IP_MULTICAST_IF, &tempRequest, &requestLength);
        return (rc == -1) ? -1 : tempRequest.imr_ifindex;
    } else if (family == AF_INET6) {
        // IPV6_MULTICAST_IF returns a pointer to an integer.
        int interfaceIndex;
        socklen_t requestLength = sizeof(interfaceIndex);
        int rc = getsockopt(socket, IPPROTO_IPV6, IPV6_MULTICAST_IF, &interfaceIndex, &requestLength);
        return (rc == -1) ? -1 : interfaceIndex;
    } else {
        errno = EAFNOSUPPORT;
        return -1;
    }
}

/**
 * Join/Leave the nominated multicast group on the specified socket.
 * Implemented by setting the multicast 'add membership'/'drop membership'
 * option at the HY_IPPROTO_IP level on the socket.
 *
 * Implementation note for multicast sockets in general:
 *
 * - This code is untested, because at the time of this writing multicast can't
 * be properly tested on Android due to GSM routing restrictions. So it might
 * or might not work.
 *
 * @param env pointer to the JNI library.
 * @param socketP pointer to the hysocket to join/leave on.
 * @param optVal pointer to the InetAddress, the multicast group to join/drop.
 *
 * @exception SocketException if an error occurs during the call
 */
static void mcastAddDropMembership(JNIEnv *env, int fd, jobject optVal, int setSockOptVal) {
    /*
     * Check whether we are getting an InetAddress or an Generic IPMreq. For now
     * we support both so that we will not break the tests. If an InetAddress
     * is passed in, only support IPv4 as obtaining an interface from an
     * InetAddress is complex and should be done by the Java caller.
     */
    if (env->IsInstanceOf(optVal, JniConstants::inetAddressClass)) {
        /*
         * optVal is an InetAddress. Construct a multicast request structure
         * from this address. Support IPv4 only.
         */
        struct ip_mreqn multicastRequest;
        socklen_t length = sizeof(multicastRequest);
        memset(&multicastRequest, 0, length);

        int interfaceIndex = interfaceIndexFromMulticastSocket(fd);
        multicastRequest.imr_ifindex = interfaceIndex;
        if (interfaceIndex == -1) {
            jniThrowSocketException(env, errno);
            return;
        }

        // Convert the inetAddress to an IPv4 address structure.
        sockaddr_storage sockaddrP;
        if (!inetAddressToSocketAddress(env, optVal, 0, &sockaddrP)) {
            return;
        }
        if (sockaddrP.ss_family != AF_INET) {
            jniThrowSocketException(env, EAFNOSUPPORT);
            return;
        }
        struct sockaddr_in *sin = (struct sockaddr_in *) &sockaddrP;
        multicastRequest.imr_multiaddr = sin->sin_addr;

        int rc = setsockopt(fd, IPPROTO_IP, setSockOptVal, &multicastRequest, length);
        if (rc == -1) {
            jniThrowSocketException(env, errno);
            return;
        }
    } else {
        /*
         * optVal is a GenericIPMreq object. Extract the relevant fields from
         * it and construct a multicast request structure from these. Support
         * both IPv4 and IPv6.
         */

        // Get the multicast address to join or leave.
        jfieldID multiaddrID = env->GetFieldID(JniConstants::genericIPMreqClass, "multiaddr", "Ljava/net/InetAddress;");
        jobject multiaddr = env->GetObjectField(optVal, multiaddrID);

        // Get the interface index to use.
        jfieldID interfaceIdxID = env->GetFieldID(JniConstants::genericIPMreqClass, "interfaceIdx", "I");
        int interfaceIndex = env->GetIntField(optVal, interfaceIdxID);
        LOGI("mcastAddDropMembership interfaceIndex=%i", interfaceIndex);

        sockaddr_storage sockaddrP;
        if (!inetAddressToSocketAddress(env, multiaddr, 0, &sockaddrP)) {
            return;
        }

        int family = getSocketAddressFamily(fd);

        // Handle IPv4 multicast on an IPv6 socket.
        if (family == AF_INET6 && sockaddrP.ss_family == AF_INET) {
            family = AF_INET;
        }

        struct ip_mreqn ipv4Request;
        struct ipv6_mreq ipv6Request;
        void *multicastRequest;
        socklen_t requestLength;
        int level;
        switch (family) {
            case AF_INET:
                requestLength = sizeof(ipv4Request);
                memset(&ipv4Request, 0, requestLength);
                ipv4Request.imr_multiaddr = ((struct sockaddr_in *) &sockaddrP)->sin_addr;
                ipv4Request.imr_ifindex = interfaceIndex;
                multicastRequest = &ipv4Request;
                level = IPPROTO_IP;
                break;
            case AF_INET6:
                // setSockOptVal is passed in by the caller and may be IPv4-only
                if (setSockOptVal == IP_ADD_MEMBERSHIP) {
                    setSockOptVal = IPV6_ADD_MEMBERSHIP;
                }
                if (setSockOptVal == IP_DROP_MEMBERSHIP) {
                    setSockOptVal = IPV6_DROP_MEMBERSHIP;
                }
                requestLength = sizeof(ipv6Request);
                memset(&ipv6Request, 0, requestLength);
                ipv6Request.ipv6mr_multiaddr = ((struct sockaddr_in6 *) &sockaddrP)->sin6_addr;
                ipv6Request.ipv6mr_interface = interfaceIndex;
                multicastRequest = &ipv6Request;
                level = IPPROTO_IPV6;
                break;
           default:
                jniThrowSocketException(env, EAFNOSUPPORT);
                return;
        }

        /* join/drop the multicast address */
        int rc = setsockopt(fd, level, setSockOptVal, multicastRequest, requestLength);
        if (rc == -1) {
            jniThrowSocketException(env, errno);
            return;
        }
    }
}
#endif // def ENABLE_MULTICAST

static bool initCachedFields(JNIEnv* env) {
    memset(&gCachedFields, 0, sizeof(gCachedFields));
    struct CachedFields *c = &gCachedFields;

    struct fieldInfo {
        jfieldID *field;
        jclass clazz;
        const char *name;
        const char *type;
    } fields[] = {
        {&c->iaddr_ipaddress, JniConstants::inetAddressClass, "ipaddress", "[B"},
        {&c->integer_class_value, JniConstants::integerClass, "value", "I"},
        {&c->boolean_class_value, JniConstants::booleanClass, "value", "Z"},
        {&c->socketimpl_port, JniConstants::socketImplClass, "port", "I"},
        {&c->socketimpl_localport, JniConstants::socketImplClass, "localport", "I"},
        {&c->socketimpl_address, JniConstants::socketImplClass, "address", "Ljava/net/InetAddress;"},
        {&c->dpack_address, JniConstants::datagramPacketClass, "address", "Ljava/net/InetAddress;"},
        {&c->dpack_port, JniConstants::datagramPacketClass, "port", "I"},
        {&c->dpack_length, JniConstants::datagramPacketClass, "length", "I"}
    };
    for (unsigned i = 0; i < sizeof(fields) / sizeof(fields[0]); i++) {
        fieldInfo f = fields[i];
        *f.field = env->GetFieldID(f.clazz, f.name, f.type);
        if (*f.field == NULL) return false;
    }
    return true;
}

static int createSocketFileDescriptor(JNIEnv* env, jobject fileDescriptor, int type) {
    if (fileDescriptor == NULL) {
        jniThrowNullPointerException(env, NULL);
        errno = EBADF;
        return -1;
    }

    // Try IPv6 but fall back to IPv4...
    int fd = socket(AF_INET6, type, 0);
    if (fd == -1 && errno == EAFNOSUPPORT) {
        fd = socket(AF_INET, type, 0);
    }
    if (fd == -1) {
        jniThrowSocketException(env, errno);
    } else {
        jniSetFileDescriptorOfFD(env, fileDescriptor, fd);
    }
    return fd;
}

static void osNetworkSystem_createStreamSocket(JNIEnv* env, jobject, jobject fileDescriptor) {
    createSocketFileDescriptor(env, fileDescriptor, SOCK_STREAM);
}

static void osNetworkSystem_createServerStreamSocket(JNIEnv* env, jobject, jobject fileDescriptor) {
    int fd = createSocketFileDescriptor(env, fileDescriptor, SOCK_STREAM);
    if (fd != -1) {
        // TODO: we could actually do this in Java. (and check for errors!)
        int value = 1;
        setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &value, sizeof(int));
    }
}

static void osNetworkSystem_createDatagramSocket(JNIEnv* env, jobject, jobject fileDescriptor) {
    int fd = createSocketFileDescriptor(env, fileDescriptor, SOCK_DGRAM);
#ifdef __linux__
    // The RFC (http://www.ietf.org/rfc/rfc3493.txt) says that IPV6_MULTICAST_HOPS defaults to 1.
    // The Linux kernel (at least up to 2.6.32) accidentally defaults to 64 (which would be correct
    // for the *unicast* hop limit). See http://www.spinics.net/lists/netdev/msg129022.html.
    // When that's fixed, we can remove this code. Until then, we manually set the hop limit on
    // IPv6 datagram sockets. (IPv4 is already correct.)
    if (fd != -1 && getSocketAddressFamily(fd) == AF_INET6) {
        int ttl = 1;
        setsockopt(fd, IPPROTO_IPV6, IPV6_MULTICAST_HOPS, &ttl, sizeof(int));
    }
#endif
}

static jint osNetworkSystem_writeDirect(JNIEnv* env, jobject,
        jobject fileDescriptor, jint address, jint offset, jint count) {
    if (count <= 0) {
        return 0;
    }

    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return 0;
    }

    jbyte* message = reinterpret_cast<jbyte*>(static_cast<uintptr_t>(address + offset));
    int bytesSent = write(fd.get(), message, count);
    if (bytesSent == -1) {
        if (errno == EAGAIN || errno == EWOULDBLOCK) {
            // We were asked to write to a non-blocking socket, but were told
            // it would block, so report "no bytes written".
            return 0;
        } else {
            jniThrowSocketException(env, errno);
            return 0;
        }
    }
    return bytesSent;
}

static jint osNetworkSystem_write(JNIEnv* env, jobject,
        jobject fileDescriptor, jbyteArray byteArray, jint offset, jint count) {
    ScopedByteArrayRW bytes(env, byteArray);
    if (bytes.get() == NULL) {
        return -1;
    }
    jint address = static_cast<jint>(reinterpret_cast<uintptr_t>(bytes.get()));
    int result = osNetworkSystem_writeDirect(env, NULL, fileDescriptor, address, offset, count);
    return result;
}

static jboolean osNetworkSystem_connectWithTimeout(JNIEnv* env,
        jobject, jobject fileDescriptor, jint timeout,
        jobject inetAddr, jint port, jint step, jbyteArray passContext) {
    sockaddr_storage address;
    if (!inetAddressToSocketAddress(env, inetAddr, port, &address)) {
        return JNI_FALSE;
    }

    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return JNI_FALSE;
    }

    ScopedByteArrayRW contextBytes(env, passContext);
    if (contextBytes.get() == NULL) {
        return JNI_FALSE;
    }
    selectFDSet* context = reinterpret_cast<selectFDSet*>(contextBytes.get());
    int result = 0;
    switch (step) {
    case SOCKET_CONNECT_STEP_START:
        result = sockConnectWithTimeout(fd.get(), address, 0, SOCKET_STEP_START, context);
        break;
    case SOCKET_CONNECT_STEP_CHECK:
        result = sockConnectWithTimeout(fd.get(), address, timeout, SOCKET_STEP_CHECK, context);
        break;
    default:
        assert(false);
    }

    if (result == 0) {
        // Connected!
        sockConnectWithTimeout(fd.get(), address, 0, SOCKET_STEP_DONE, NULL);
        return JNI_TRUE;
    }

    if (result == -EINPROGRESS) {
        // Not yet connected, but not yet denied either... Try again later.
        return JNI_FALSE;
    }

    // Denied!
    sockConnectWithTimeout(fd.get(), address, 0, SOCKET_STEP_DONE, NULL);
    if (result == -EACCES) {
        jniThrowSecurityException(env, -result);
    } else {
        jniThrowConnectException(env, -result);
    }
    return JNI_FALSE;
}

static void osNetworkSystem_connectStreamWithTimeoutSocket(JNIEnv* env,
        jobject, jobject fileDescriptor, jint remotePort, jint timeout, jobject inetAddr) {
    int result = 0;
    sockaddr_storage address;
    int remainingTimeout = timeout;
    int passedTimeout = 0;
    int finishTime = 0;
    bool hasTimeout = timeout > 0;

    /* if a timeout was specified calculate the finish time value */
    if (hasTimeout)  {
        finishTime = time_msec_clock() + (int) timeout;
    }

    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return;
    }

    if (!inetAddressToSocketAddress(env, inetAddr, remotePort, &address)) {
        return;
    }

    /*
     * we will be looping checking for when we are connected so allocate
     * the descriptor sets that we will use
     */
    selectFDSet context;
    result = sockConnectWithTimeout(fd.get(), address, 0, SOCKET_STEP_START, &context);
    if (result == 0) {
        /* ok we connected right away so we are done */
        sockConnectWithTimeout(fd.get(), address, 0, SOCKET_STEP_DONE, &context);
        return;
    } else if (result != -EINPROGRESS) {
        sockConnectWithTimeout(fd.get(), address, 0, SOCKET_STEP_DONE, &context);
        /* we got an error other than NOTCONNECTED so we cannot continue */
        if (result == -EACCES) {
            jniThrowSecurityException(env, -result);
        } else {
            jniThrowSocketException(env, -result);
        }
        return;
    }

    while (result == -EINPROGRESS) {
        passedTimeout = remainingTimeout;

        /*
         * ok now try and connect. Depending on the platform this may sleep
         * for up to passedTimeout milliseconds
         */
        result = sockConnectWithTimeout(fd.get(), address, passedTimeout, SOCKET_STEP_CHECK, &context);

        /*
         * now check if the socket is still connected.
         * Do it here as some platforms seem to think they
         * are connected if the socket is closed on them.
         */
        if (fd.isClosed()) {
            sockConnectWithTimeout(fd.get(), address, 0, SOCKET_STEP_DONE, &context);
            return;
        }

        /*
         * check if we are now connected,
         * if so we can finish the process and return
         */
        if (result == 0) {
            sockConnectWithTimeout(fd.get(), address, 0, SOCKET_STEP_DONE, &context);
            return;
        }

        /*
         * if the error is -EINPROGRESS then we have not yet
         * connected and we may not be done yet
         */
        if (result == -EINPROGRESS) {
            /* check if the timeout has expired */
            if (hasTimeout) {
                remainingTimeout = finishTime - time_msec_clock();
                if (remainingTimeout <= 0) {
                    sockConnectWithTimeout(fd.get(), address, 0, SOCKET_STEP_DONE, &context);
                    jniThrowSocketTimeoutException(env, ETIMEDOUT);
                    return;
                }
            } else {
                remainingTimeout = 100;
            }
        } else {
            sockConnectWithTimeout(fd.get(), address, remainingTimeout, SOCKET_STEP_DONE, &context);
            if (result == -ECONNRESET || result == -ECONNREFUSED || result == -EADDRNOTAVAIL ||
                    result == -EADDRINUSE || result == -ENETUNREACH) {
                jniThrowConnectException(env, -result);
            } else if (result == -EACCES) {
                jniThrowSecurityException(env, -result);
            } else {
                jniThrowSocketException(env, -result);
            }
            return;
        }
    }
}

static void osNetworkSystem_bind(JNIEnv* env, jobject, jobject fileDescriptor,
        jobject inetAddress, jint port) {
    sockaddr_storage socketAddress;
    if (!inetAddressToSocketAddress(env, inetAddress, port, &socketAddress)) {
        return;
    }

    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return;
    }

    const CompatibleSocketAddress compatibleAddress(fd.get(), socketAddress, false);
    int rc = TEMP_FAILURE_RETRY(bind(fd.get(), compatibleAddress.get(), sizeof(sockaddr_storage)));
    if (rc == -1) {
        jniThrowBindException(env, errno);
    }
}

static void osNetworkSystem_listen(JNIEnv* env, jobject, jobject fileDescriptor, jint backlog) {
    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return;
    }

    int rc = listen(fd.get(), backlog);
    if (rc == -1) {
        jniThrowSocketException(env, errno);
    }
}

static void osNetworkSystem_accept(JNIEnv* env, jobject, jobject serverFileDescriptor,
        jobject newSocket, jobject clientFileDescriptor) {

    if (newSocket == NULL) {
        jniThrowNullPointerException(env, NULL);
        return;
    }

    NetFd serverFd(env, serverFileDescriptor);
    if (serverFd.isClosed()) {
        return;
    }

    sockaddr_storage ss;
    socklen_t addrLen = sizeof(ss);
    sockaddr* sa = reinterpret_cast<sockaddr*>(&ss);
    int clientFd = NET_FAILURE_RETRY(env, serverFd, accept(serverFd.get(), sa, &addrLen));
    if (clientFd == -1) {
        return;
    }

    // Reset the inherited read timeout to the Java-specified default of 0.
    timeval timeout(toTimeval(0));
    int rc = setsockopt(clientFd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    if (rc == -1) {
        LOGE("couldn't reset SO_RCVTIMEO on accepted socket: %s (%i)", strerror(errno), errno);
        jniThrowSocketException(env, errno);
    }

    /*
     * For network sockets, put the peer address and port in instance variables.
     * We don't bother to do this for UNIX domain sockets, since most peers are
     * anonymous anyway.
     */
    if (ss.ss_family == AF_INET || ss.ss_family == AF_INET6) {
        // Remote address and port.
        jobject remoteAddress = socketAddressToInetAddress(env, &ss);
        if (remoteAddress == NULL) {
            close(clientFd);
            return;
        }
        int remotePort = getSocketAddressPort(&ss);
        env->SetObjectField(newSocket, gCachedFields.socketimpl_address, remoteAddress);
        env->SetIntField(newSocket, gCachedFields.socketimpl_port, remotePort);

        // Local port.
        memset(&ss, 0, addrLen);
        int rc = getsockname(clientFd, sa, &addrLen);
        if (rc == -1) {
            close(clientFd);
            jniThrowSocketException(env, errno);
            return;
        }
        int localPort = getSocketAddressPort(&ss);
        env->SetIntField(newSocket, gCachedFields.socketimpl_localport, localPort);
    }

    jniSetFileDescriptorOfFD(env, clientFileDescriptor, clientFd);
}

static void osNetworkSystem_sendUrgentData(JNIEnv* env, jobject,
        jobject fileDescriptor, jbyte value) {
    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return;
    }

    int rc = send(fd.get(), &value, 1, MSG_OOB);
    if (rc == -1) {
        jniThrowSocketException(env, errno);
    }
}

static void osNetworkSystem_connectDatagram(JNIEnv* env, jobject,
        jobject fileDescriptor, jint port, jobject inetAddress) {
    sockaddr_storage sockAddr;
    if (!inetAddressToSocketAddress(env, inetAddress, port, &sockAddr)) {
        return;
    }

    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return;
    }

    if (doConnect(fd.get(), &sockAddr) == -1) {
        jniThrowSocketException(env, errno);
    }
}

static void osNetworkSystem_disconnectDatagram(JNIEnv* env, jobject, jobject fileDescriptor) {
    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return;
    }

    // To disconnect a datagram socket, we connect to a bogus address with
    // the family AF_UNSPEC.
    sockaddr_storage ss;
    memset(&ss, 0, sizeof(ss));
    ss.ss_family = AF_UNSPEC;
    const sockaddr* sa = reinterpret_cast<const sockaddr*>(&ss);
    int rc = TEMP_FAILURE_RETRY(connect(fd.get(), sa, sizeof(ss)));
    if (rc == -1) {
        jniThrowSocketException(env, errno);
    }
}

static void osNetworkSystem_setInetAddress(JNIEnv* env, jobject,
        jobject sender, jbyteArray address) {
    env->SetObjectField(sender, gCachedFields.iaddr_ipaddress, address);
}

// TODO: can we merge this with recvDirect?
static jint osNetworkSystem_readDirect(JNIEnv* env, jobject, jobject fileDescriptor,
        jint address, jint count) {
    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return 0;
    }

    jbyte* dst = reinterpret_cast<jbyte*>(static_cast<uintptr_t>(address));
    while (true) {
        ssize_t bytesReceived = read(fd.get(), dst, count);
        if (bytesReceived == 0) {
            // If fd is closed, we saw EOF because of the shutdown(2) that happens at close...
            if (fd.isClosed()) {
                return 0;
            }
            // ...otherwise it was a genuine EOF.
            return -1;
        } else if (bytesReceived == -1) {
            if (errno == EINTR) {
                continue;
            }
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                // We were asked to read a non-blocking socket with no data
                // available, so report "no bytes read".
                return 0;
            } else {
                jniThrowSocketException(env, errno);
                return 0;
            }
        } else {
            return bytesReceived;
        }
    }
}

static jint osNetworkSystem_read(JNIEnv* env, jclass, jobject fileDescriptor,
        jbyteArray byteArray, jint offset, jint count) {
    ScopedByteArrayRW bytes(env, byteArray);
    if (bytes.get() == NULL) {
        return -1;
    }
    jint address = static_cast<jint>(reinterpret_cast<uintptr_t>(bytes.get() + offset));
    return osNetworkSystem_readDirect(env, NULL, fileDescriptor, address, count);
}

// TODO: can we merge this with readDirect?
static jint osNetworkSystem_recvDirect(JNIEnv* env, jobject, jobject fileDescriptor, jobject packet,
        jint address, jint offset, jint length, jboolean peek, jboolean connected) {
    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return 0;
    }

    char* buf = reinterpret_cast<char*>(static_cast<uintptr_t>(address + offset));
    const int flags = peek ? MSG_PEEK : 0;
    sockaddr_storage ss;
    memset(&ss, 0, sizeof(ss));
    socklen_t sockAddrLen = sizeof(ss);
    sockaddr* from = connected ? NULL : reinterpret_cast<sockaddr*>(&ss);
    socklen_t* fromLength = connected ? NULL : &sockAddrLen;
    ssize_t actualLength = TEMP_FAILURE_RETRY(recvfrom(fd.get(), buf, length, flags, from, fromLength));
    if (actualLength == -1) {
        if (connected && errno == ECONNREFUSED) {
            jniThrowException(env, "java/net/PortUnreachableException", "");
        } else if (errno == EAGAIN || errno == EWOULDBLOCK) {
            jniThrowSocketTimeoutException(env, errno);
        } else {
            jniThrowSocketException(env, errno);
        }
        return 0;
    }

    if (packet != NULL) {
        env->SetIntField(packet, gCachedFields.dpack_length, actualLength);
        if (!connected) {
            jbyteArray addr = socketAddressToByteArray(env, &ss);
            if (addr == NULL) {
                return 0;
            }
            int port = getSocketAddressPort(&ss);
            jobject sender = byteArrayToInetAddress(env, addr);
            if (sender == NULL) {
                return 0;
            }
            env->SetObjectField(packet, gCachedFields.dpack_address, sender);
            env->SetIntField(packet, gCachedFields.dpack_port, port);
        }
    }
    return actualLength;
}

static jint osNetworkSystem_recv(JNIEnv* env, jobject, jobject fd, jobject packet,
        jbyteArray javaBytes, jint offset, jint length, jboolean peek, jboolean connected) {
    ScopedByteArrayRW bytes(env, javaBytes);
    if (bytes.get() == NULL) {
        return -1;
    }
    uintptr_t address = reinterpret_cast<uintptr_t>(bytes.get());
    return osNetworkSystem_recvDirect(env, NULL, fd, packet, address, offset, length, peek,
            connected);
}








static jint osNetworkSystem_sendDirect(JNIEnv* env, jobject, jobject fileDescriptor, jint address, jint offset, jint length, jint port, jobject inetAddress) {
    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return -1;
    }

    sockaddr_storage receiver;
    if (inetAddress != NULL && !inetAddressToSocketAddress(env, inetAddress, port, &receiver)) {
        return -1;
    }

    int flags = 0;
    char* buf = reinterpret_cast<char*>(static_cast<uintptr_t>(address + offset));
    sockaddr* to = inetAddress ? reinterpret_cast<sockaddr*>(&receiver) : NULL;
    socklen_t toLength = inetAddress ? sizeof(receiver) : 0;
    ssize_t bytesSent = TEMP_FAILURE_RETRY(sendto(fd.get(), buf, length, flags, to, toLength));
    if (bytesSent == -1) {
        if (errno == ECONNRESET || errno == ECONNREFUSED) {
            return 0;
        } else {
            jniThrowSocketException(env, errno);
        }
    }
    return bytesSent;
}

static jint osNetworkSystem_send(JNIEnv* env, jobject, jobject fd,
        jbyteArray data, jint offset, jint length,
        jint port, jobject inetAddress) {
    ScopedByteArrayRO bytes(env, data);
    if (bytes.get() == NULL) {
        return -1;
    }
    return osNetworkSystem_sendDirect(env, NULL, fd,
            reinterpret_cast<uintptr_t>(bytes.get()), offset, length, port, inetAddress);
}








static bool isValidFd(int fd) {
    return fd >= 0 && fd < FD_SETSIZE;
}

static bool initFdSet(JNIEnv* env, jobjectArray fdArray, jint count, fd_set* fdSet, int* maxFd) {
    for (int i = 0; i < count; ++i) {
        jobject fileDescriptor = env->GetObjectArrayElement(fdArray, i);
        if (fileDescriptor == NULL) {
            return false;
        }

        const int fd = jniGetFDFromFileDescriptor(env, fileDescriptor);
        if (!isValidFd(fd)) {
            LOGE("selectImpl: ignoring invalid fd %i", fd);
            continue;
        }

        FD_SET(fd, fdSet);

        if (fd > *maxFd) {
            *maxFd = fd;
        }
    }
    return true;
}

/*
 * Note: fdSet has to be non-const because although on Linux FD_ISSET() is sane
 * and takes a const fd_set*, it takes fd_set* on Mac OS. POSIX is not on our
 * side here:
 *   http://www.opengroup.org/onlinepubs/000095399/functions/select.html
 */
static bool translateFdSet(JNIEnv* env, jobjectArray fdArray, jint count, fd_set& fdSet, jint* flagArray, size_t offset, jint op) {
    for (int i = 0; i < count; ++i) {
        jobject fileDescriptor = env->GetObjectArrayElement(fdArray, i);
        if (fileDescriptor == NULL) {
            return false;
        }

        const int fd = jniGetFDFromFileDescriptor(env, fileDescriptor);
        if (isValidFd(fd) && FD_ISSET(fd, &fdSet)) {
            flagArray[i + offset] = op;
        } else {
            flagArray[i + offset] = SOCKET_OP_NONE;
        }
    }
    return true;
}

static jboolean osNetworkSystem_selectImpl(JNIEnv* env, jclass,
        jobjectArray readFDArray, jobjectArray writeFDArray, jint countReadC,
        jint countWriteC, jintArray outFlags, jlong timeoutMs) {
    // LOGD("ENTER selectImpl");

    // Initialize the fd_sets.
    int maxFd = -1;
    fd_set readFds;
    fd_set writeFds;
    FD_ZERO(&readFds);
    FD_ZERO(&writeFds);
    bool initialized = initFdSet(env, readFDArray, countReadC, &readFds, &maxFd) &&
                       initFdSet(env, writeFDArray, countWriteC, &writeFds, &maxFd);
    if (!initialized) {
        return -1;
    }

    // Initialize the timeout, if any.
    timeval tv;
    timeval* tvp = NULL;
    if (timeoutMs >= 0) {
        tv = toTimeval(timeoutMs);
        tvp = &tv;
    }

    // Perform the select.
    int result = select(maxFd + 1, &readFds, &writeFds, NULL, tvp);
    if (result == 0) {
        // Timeout.
        return JNI_FALSE;
    } else if (result == -1) {
        // Error.
        if (errno == EINTR) {
            return JNI_FALSE;
        } else {
            jniThrowSocketException(env, errno);
            return JNI_FALSE;
        }
    }

    // Translate the result into the int[] we're supposed to fill in.
    ScopedIntArrayRW flagArray(env, outFlags);
    if (flagArray.get() == NULL) {
        return JNI_FALSE;
    }
    return translateFdSet(env, readFDArray, countReadC, readFds, flagArray.get(), 0, SOCKET_OP_READ) &&
            translateFdSet(env, writeFDArray, countWriteC, writeFds, flagArray.get(), countReadC, SOCKET_OP_WRITE);
}

static jobject osNetworkSystem_getSocketLocalAddress(JNIEnv* env,
        jobject, jobject fileDescriptor) {
    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return NULL;
    }

    sockaddr_storage addr;
    socklen_t addrLen = sizeof(addr);
    memset(&addr, 0, addrLen);
    int rc = getsockname(fd.get(), (sockaddr*) &addr, &addrLen);
    if (rc == -1) {
        // TODO: the public API doesn't allow failure, so this whole method
        // represents a broken design. In practice, though, getsockname can't
        // fail unless we give it invalid arguments.
        LOGE("getsockname failed: %s (errno=%i)", strerror(errno), errno);
        return NULL;
    }
    return socketAddressToInetAddress(env, &addr);
}

static jint osNetworkSystem_getSocketLocalPort(JNIEnv* env, jobject,
        jobject fileDescriptor) {
    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return 0;
    }

    sockaddr_storage addr;
    socklen_t addrLen = sizeof(addr);
    memset(&addr, 0, addrLen);
    int rc = getsockname(fd.get(), (sockaddr*) &addr, &addrLen);
    if (rc == -1) {
        // TODO: the public API doesn't allow failure, so this whole method
        // represents a broken design. In practice, though, getsockname can't
        // fail unless we give it invalid arguments.
        LOGE("getsockname failed: %s (errno=%i)", strerror(errno), errno);
        return 0;
    }
    return getSocketAddressPort(&addr);
}

template <typename T>
static bool getSocketOption(JNIEnv* env, const NetFd& fd, int level, int option, T* value) {
    socklen_t size = sizeof(*value);
    int rc = getsockopt(fd.get(), level, option, value, &size);
    if (rc == -1) {
        LOGE("getSocketOption(fd=%i, level=%i, option=%i) failed: %s (errno=%i)",
                fd.get(), level, option, strerror(errno), errno);
        jniThrowSocketException(env, errno);
        return false;
    }
    return true;
}

static jobject getSocketOption_Boolean(JNIEnv* env, const NetFd& fd, int level, int option) {
    int value;
    return getSocketOption(env, fd, level, option, &value) ? booleanValueOf(env, value) : NULL;
}

static jobject getSocketOption_Integer(JNIEnv* env, const NetFd& fd, int level, int option) {
    int value;
    return getSocketOption(env, fd, level, option, &value) ? integerValueOf(env, value) : NULL;
}

static jobject osNetworkSystem_getSocketOption(JNIEnv* env, jobject, jobject fileDescriptor, jint option) {
    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return NULL;
    }

    int family = getSocketAddressFamily(fd.get());
    if (family != AF_INET && family != AF_INET6) {
        jniThrowSocketException(env, EAFNOSUPPORT);
        return NULL;
    }

    switch (option) {
    case JAVASOCKOPT_TCP_NODELAY:
        return getSocketOption_Boolean(env, fd, IPPROTO_TCP, TCP_NODELAY);
    case JAVASOCKOPT_SO_SNDBUF:
        return getSocketOption_Integer(env, fd, SOL_SOCKET, SO_SNDBUF);
    case JAVASOCKOPT_SO_RCVBUF:
        return getSocketOption_Integer(env, fd, SOL_SOCKET, SO_RCVBUF);
    case JAVASOCKOPT_SO_BROADCAST:
        return getSocketOption_Boolean(env, fd, SOL_SOCKET, SO_BROADCAST);
    case JAVASOCKOPT_SO_REUSEADDR:
        return getSocketOption_Boolean(env, fd, SOL_SOCKET, SO_REUSEADDR);
    case JAVASOCKOPT_SO_KEEPALIVE:
        return getSocketOption_Boolean(env, fd, SOL_SOCKET, SO_KEEPALIVE);
    case JAVASOCKOPT_SO_OOBINLINE:
        return getSocketOption_Boolean(env, fd, SOL_SOCKET, SO_OOBINLINE);
    case JAVASOCKOPT_IP_TOS:
        if (family == AF_INET) {
            return getSocketOption_Integer(env, fd, IPPROTO_IP, IP_TOS);
        } else {
            return getSocketOption_Integer(env, fd, IPPROTO_IPV6, IPV6_TCLASS);
        }
    case JAVASOCKOPT_SO_LINGER:
        {
            linger lingr;
            bool ok = getSocketOption(env, fd, SOL_SOCKET, SO_LINGER, &lingr);
            if (!ok) {
                return NULL; // We already threw.
            } else if (!lingr.l_onoff) {
                return booleanValueOf(env, false);
            } else {
                return integerValueOf(env, lingr.l_linger);
            }
        }
    case JAVASOCKOPT_SO_TIMEOUT:
        {
            timeval timeout;
            bool ok = getSocketOption(env, fd, SOL_SOCKET, SO_RCVTIMEO, &timeout);
            return ok ? integerValueOf(env, toMs(timeout)) : NULL;
        }
#ifdef ENABLE_MULTICAST
    case JAVASOCKOPT_IP_MULTICAST_IF:
        {
            sockaddr_storage sockVal;
            if (!getSocketOption(env, fd, IPPROTO_IP, IP_MULTICAST_IF, &sockVal)) {
                return NULL;
            }
            if (sockVal.ss_family != AF_INET) {
                LOGE("sockVal.ss_family != AF_INET (%i)", sockVal.ss_family);
                // Java expects an AF_INET INADDR_ANY, but Linux just returns AF_UNSPEC.
                jbyteArray inAddrAny = env->NewByteArray(4); // { 0, 0, 0, 0 }
                return byteArrayToInetAddress(env, inAddrAny);
            }
            return socketAddressToInetAddress(env, &sockVal);
        }
    case JAVASOCKOPT_IP_MULTICAST_IF2:
        if (family == AF_INET) {
            struct ip_mreqn multicastRequest;
            bool ok = getSocketOption(env, fd, IPPROTO_IP, IP_MULTICAST_IF, &multicastRequest);
            return ok ? integerValueOf(env, multicastRequest.imr_ifindex) : NULL;
        } else {
            return getSocketOption_Integer(env, fd, IPPROTO_IPV6, IPV6_MULTICAST_IF);
        }
    case JAVASOCKOPT_IP_MULTICAST_LOOP:
        if (family == AF_INET) {
            // Although IPv6 was cleaned up to use int, IPv4 multicast loopback uses a byte.
            u_char loopback;
            bool ok = getSocketOption(env, fd, IPPROTO_IP, IP_MULTICAST_LOOP, &loopback);
            return ok ? booleanValueOf(env, loopback) : NULL;
        } else {
            return getSocketOption_Boolean(env, fd, IPPROTO_IPV6, IPV6_MULTICAST_LOOP);
        }
    case JAVASOCKOPT_MULTICAST_TTL:
        if (family == AF_INET) {
            // Although IPv6 was cleaned up to use int, and IPv4 non-multicast TTL uses int,
            // IPv4 multicast TTL uses a byte.
            u_char ttl;
            bool ok = getSocketOption(env, fd, IPPROTO_IP, IP_MULTICAST_TTL, &ttl);
            return ok ? integerValueOf(env, ttl) : NULL;
        } else {
            return getSocketOption_Integer(env, fd, IPPROTO_IPV6, IPV6_MULTICAST_HOPS);
        }
#else
    case JAVASOCKOPT_MULTICAST_TTL:
    case JAVASOCKOPT_IP_MULTICAST_IF:
    case JAVASOCKOPT_IP_MULTICAST_IF2:
    case JAVASOCKOPT_IP_MULTICAST_LOOP:
        jniThrowException(env, "java/lang/UnsupportedOperationException", NULL);
        return NULL;
#endif // def ENABLE_MULTICAST
    default:
        jniThrowSocketException(env, ENOPROTOOPT);
        return NULL;
    }
}

template <typename T>
static void setSocketOption(JNIEnv* env, const NetFd& fd, int level, int option, T* value) {
    int rc = setsockopt(fd.get(), level, option, value, sizeof(*value));
    if (rc == -1) {
        LOGE("setSocketOption(fd=%i, level=%i, option=%i) failed: %s (errno=%i)",
                fd.get(), level, option, strerror(errno), errno);
        jniThrowSocketException(env, errno);
    }
}

static void osNetworkSystem_setSocketOption(JNIEnv* env, jobject, jobject fileDescriptor, jint option, jobject optVal) {
    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return;
    }

    int intVal;
    bool wasBoolean = false;
    if (env->IsInstanceOf(optVal, JniConstants::integerClass)) {
        intVal = (int) env->GetIntField(optVal, gCachedFields.integer_class_value);
    } else if (env->IsInstanceOf(optVal, JniConstants::booleanClass)) {
        intVal = (int) env->GetBooleanField(optVal, gCachedFields.boolean_class_value);
        wasBoolean = true;
    } else if (env->IsInstanceOf(optVal, JniConstants::genericIPMreqClass) || env->IsInstanceOf(optVal, JniConstants::inetAddressClass)) {
        // we'll use optVal directly
    } else {
        jniThrowSocketException(env, EINVAL);
        return;
    }

    int family = getSocketAddressFamily(fd.get());
    if (family != AF_INET && family != AF_INET6) {
        jniThrowSocketException(env, EAFNOSUPPORT);
        return;
    }

    // Since we expect to have a AF_INET6 socket even if we're communicating via IPv4, we always
    // set the IPPROTO_IP options. As long as we fall back to creating IPv4 sockets if creating
    // an IPv6 socket fails, we need to make setting the IPPROTO_IPV6 options conditional.
    switch (option) {
    case JAVASOCKOPT_IP_TOS:
        setSocketOption(env, fd, IPPROTO_IP, IP_TOS, &intVal);
        if (family == AF_INET6) {
            setSocketOption(env, fd, IPPROTO_IPV6, IPV6_TCLASS, &intVal);
        }
        return;
    case JAVASOCKOPT_SO_BROADCAST:
        setSocketOption(env, fd, SOL_SOCKET, SO_BROADCAST, &intVal);
        return;
    case JAVASOCKOPT_SO_KEEPALIVE:
        setSocketOption(env, fd, SOL_SOCKET, SO_KEEPALIVE, &intVal);
        return;
    case JAVASOCKOPT_SO_LINGER:
        {
            linger l;
            l.l_onoff = !wasBoolean;
            l.l_linger = intVal <= 65535 ? intVal : 65535;
            setSocketOption(env, fd, SOL_SOCKET, SO_LINGER, &l);
            return;
        }
    case JAVASOCKOPT_SO_OOBINLINE:
        setSocketOption(env, fd, SOL_SOCKET, SO_OOBINLINE, &intVal);
        return;
    case JAVASOCKOPT_SO_RCVBUF:
        setSocketOption(env, fd, SOL_SOCKET, SO_RCVBUF, &intVal);
        return;
    case JAVASOCKOPT_SO_REUSEADDR:
        setSocketOption(env, fd, SOL_SOCKET, SO_REUSEADDR, &intVal);
        return;
    case JAVASOCKOPT_SO_SNDBUF:
        setSocketOption(env, fd, SOL_SOCKET, SO_SNDBUF, &intVal);
        return;
    case JAVASOCKOPT_SO_TIMEOUT:
        {
            timeval timeout(toTimeval(intVal));
            setSocketOption(env, fd, SOL_SOCKET, SO_RCVTIMEO, &timeout);
            return;
        }
    case JAVASOCKOPT_TCP_NODELAY:
        setSocketOption(env, fd, IPPROTO_TCP, TCP_NODELAY, &intVal);
        return;
#ifdef ENABLE_MULTICAST
    case JAVASOCKOPT_MCAST_ADD_MEMBERSHIP:
        mcastAddDropMembership(env, fd.get(), optVal, IP_ADD_MEMBERSHIP);
        return;
    case JAVASOCKOPT_MCAST_DROP_MEMBERSHIP:
        mcastAddDropMembership(env, fd.get(), optVal, IP_DROP_MEMBERSHIP);
        return;
    case JAVASOCKOPT_IP_MULTICAST_IF:
        {
            sockaddr_storage sockVal;
            if (!env->IsInstanceOf(optVal, JniConstants::inetAddressClass) ||
                    !inetAddressToSocketAddress(env, optVal, 0, &sockVal)) {
                return;
            }
            // This call is IPv4 only. The socket may be IPv6, but the address
            // that identifies the interface to join must be an IPv4 address.
            if (sockVal.ss_family != AF_INET) {
                jniThrowSocketException(env, EAFNOSUPPORT);
                return;
            }
            struct ip_mreqn mcast_req;
            memset(&mcast_req, 0, sizeof(mcast_req));
            mcast_req.imr_address = reinterpret_cast<sockaddr_in*>(&sockVal)->sin_addr;
            setSocketOption(env, fd, IPPROTO_IP, IP_MULTICAST_IF, &mcast_req);
            return;
        }
    case JAVASOCKOPT_IP_MULTICAST_IF2:
        // TODO: is this right? should we unconditionally set the IPPROTO_IP state in case
        // we have an IPv6 socket communicating via IPv4?
        if (family == AF_INET) {
            // IP_MULTICAST_IF expects a pointer to a struct ip_mreqn.
            struct ip_mreqn multicastRequest;
            memset(&multicastRequest, 0, sizeof(multicastRequest));
            multicastRequest.imr_ifindex = intVal;
            setSocketOption(env, fd, IPPROTO_IP, IP_MULTICAST_IF, &multicastRequest);
        } else {
            // IPV6_MULTICAST_IF expects a pointer to an integer.
            setSocketOption(env, fd, IPPROTO_IPV6, IPV6_MULTICAST_IF, &intVal);
        }
        return;
    case JAVASOCKOPT_MULTICAST_TTL:
        {
            // Although IPv6 was cleaned up to use int, and IPv4 non-multicast TTL uses int,
            // IPv4 multicast TTL uses a byte.
            u_char ttl = intVal;
            setSocketOption(env, fd, IPPROTO_IP, IP_MULTICAST_TTL, &ttl);
            if (family == AF_INET6) {
                setSocketOption(env, fd, IPPROTO_IPV6, IPV6_MULTICAST_HOPS, &intVal);
            }
            return;
        }
    case JAVASOCKOPT_IP_MULTICAST_LOOP:
        {
            // Although IPv6 was cleaned up to use int, IPv4 multicast loopback uses a byte.
            u_char loopback = intVal;
            setSocketOption(env, fd, IPPROTO_IP, IP_MULTICAST_LOOP, &loopback);
            if (family == AF_INET6) {
                setSocketOption(env, fd, IPPROTO_IPV6, IPV6_MULTICAST_LOOP, &intVal);
            }
            return;
        }
#else
    case JAVASOCKOPT_MULTICAST_TTL:
    case JAVASOCKOPT_MCAST_ADD_MEMBERSHIP:
    case JAVASOCKOPT_MCAST_DROP_MEMBERSHIP:
    case JAVASOCKOPT_IP_MULTICAST_IF:
    case JAVASOCKOPT_IP_MULTICAST_IF2:
    case JAVASOCKOPT_IP_MULTICAST_LOOP:
        jniThrowException(env, "java/lang/UnsupportedOperationException", NULL);
        return;
#endif // def ENABLE_MULTICAST
    default:
        jniThrowSocketException(env, ENOPROTOOPT);
    }
}

static void doShutdown(JNIEnv* env, jobject fileDescriptor, int how) {
    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return;
    }
    int rc = shutdown(fd.get(), how);
    if (rc == -1) {
        jniThrowSocketException(env, errno);
    }
}

static void osNetworkSystem_shutdownInput(JNIEnv* env, jobject, jobject fd) {
    doShutdown(env, fd, SHUT_RD);
}

static void osNetworkSystem_shutdownOutput(JNIEnv* env, jobject, jobject fd) {
    doShutdown(env, fd, SHUT_WR);
}

static void osNetworkSystem_close(JNIEnv* env, jobject, jobject fileDescriptor) {
    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return;
    }

    jniSetFileDescriptorOfFD(env, fileDescriptor, -1);

    // TODO: this will be part of the fix for http://b/2823977.
    // shutdown(fd.get(), SHUT_RDWR);

    close(fd.get());
}

static JNINativeMethod gMethods[] = {
    { "accept",                         "(Ljava/io/FileDescriptor;Ljava/net/SocketImpl;Ljava/io/FileDescriptor;)V", (void*) osNetworkSystem_accept },
    { "bind",                           "(Ljava/io/FileDescriptor;Ljava/net/InetAddress;I)V",                       (void*) osNetworkSystem_bind },
    { "close",                          "(Ljava/io/FileDescriptor;)V",                                              (void*) osNetworkSystem_close },
    { "connectDatagram",                "(Ljava/io/FileDescriptor;ILjava/net/InetAddress;)V",                       (void*) osNetworkSystem_connectDatagram },
    { "connectStreamWithTimeoutSocket", "(Ljava/io/FileDescriptor;IILjava/net/InetAddress;)V",                      (void*) osNetworkSystem_connectStreamWithTimeoutSocket },
    { "connectWithTimeout",             "(Ljava/io/FileDescriptor;ILjava/net/InetAddress;II[B)Z",                   (void*) osNetworkSystem_connectWithTimeout },
    { "createDatagramSocket",           "(Ljava/io/FileDescriptor;)V",                                              (void*) osNetworkSystem_createDatagramSocket },
    { "createServerStreamSocket",       "(Ljava/io/FileDescriptor;)V",                                              (void*) osNetworkSystem_createServerStreamSocket },
    { "createStreamSocket",             "(Ljava/io/FileDescriptor;)V",                                              (void*) osNetworkSystem_createStreamSocket },
    { "disconnectDatagram",             "(Ljava/io/FileDescriptor;)V",                                              (void*) osNetworkSystem_disconnectDatagram },
    { "getSocketLocalAddress",          "(Ljava/io/FileDescriptor;)Ljava/net/InetAddress;",                         (void*) osNetworkSystem_getSocketLocalAddress },
    { "getSocketLocalPort",             "(Ljava/io/FileDescriptor;)I",                                              (void*) osNetworkSystem_getSocketLocalPort },
    { "getSocketOption",                "(Ljava/io/FileDescriptor;I)Ljava/lang/Object;",                            (void*) osNetworkSystem_getSocketOption },
    { "listen",                         "(Ljava/io/FileDescriptor;I)V",                                             (void*) osNetworkSystem_listen },
    { "read",                           "(Ljava/io/FileDescriptor;[BII)I",                                          (void*) osNetworkSystem_read },
    { "readDirect",                     "(Ljava/io/FileDescriptor;II)I",                                            (void*) osNetworkSystem_readDirect },
    { "recv",                           "(Ljava/io/FileDescriptor;Ljava/net/DatagramPacket;[BIIZZ)I",               (void*) osNetworkSystem_recv },
    { "recvDirect",                     "(Ljava/io/FileDescriptor;Ljava/net/DatagramPacket;IIIZZ)I",                (void*) osNetworkSystem_recvDirect },
    { "selectImpl",                     "([Ljava/io/FileDescriptor;[Ljava/io/FileDescriptor;II[IJ)Z",               (void*) osNetworkSystem_selectImpl },
    { "send",                           "(Ljava/io/FileDescriptor;[BIIILjava/net/InetAddress;)I",                   (void*) osNetworkSystem_send },
    { "sendDirect",                     "(Ljava/io/FileDescriptor;IIIILjava/net/InetAddress;)I",                    (void*) osNetworkSystem_sendDirect },
    { "sendUrgentData",                 "(Ljava/io/FileDescriptor;B)V",                                             (void*) osNetworkSystem_sendUrgentData },
    { "setInetAddress",                 "(Ljava/net/InetAddress;[B)V",                                              (void*) osNetworkSystem_setInetAddress },
    { "setSocketOption",                "(Ljava/io/FileDescriptor;ILjava/lang/Object;)V",                           (void*) osNetworkSystem_setSocketOption },
    { "shutdownInput",                  "(Ljava/io/FileDescriptor;)V",                                              (void*) osNetworkSystem_shutdownInput },
    { "shutdownOutput",                 "(Ljava/io/FileDescriptor;)V",                                              (void*) osNetworkSystem_shutdownOutput },
    { "write",                          "(Ljava/io/FileDescriptor;[BII)I",                                          (void*) osNetworkSystem_write },
    { "writeDirect",                    "(Ljava/io/FileDescriptor;III)I",                                           (void*) osNetworkSystem_writeDirect },
};
int register_org_apache_harmony_luni_platform_OSNetworkSystem(JNIEnv* env) {
    return initCachedFields(env) && jniRegisterNativeMethods(env,
            "org/apache/harmony/luni/platform/OSNetworkSystem", gMethods, NELEM(gMethods));
}
