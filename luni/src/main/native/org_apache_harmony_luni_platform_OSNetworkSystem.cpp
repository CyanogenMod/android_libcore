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

#include "AsynchronousSocketCloseMonitor.h"
#include "JNIHelp.h"
#include "JniConstants.h"
#include "JniException.h"
#include "NetFd.h"
#include "NetworkUtilities.h"
#include "ScopedLocalRef.h"
#include "ScopedPrimitiveArray.h"
#include "jni.h"
#include "valueOf.h"

#include <arpa/inet.h>
#include <errno.h>
#include <net/if.h>
#include <netdb.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <unistd.h>

static void jniThrowSocketTimeoutException(JNIEnv* env, int error) {
    jniThrowExceptionWithErrno(env, "java/net/SocketTimeoutException", error);
}

/**
 * Returns the port number in a sockaddr_storage structure.
 *
 * @param address the sockaddr_storage structure to get the port from
 *
 * @return the port number, or -1 if the address family is unknown.
 */
static int getSocketAddressPort(sockaddr_storage* ss) {
    switch (ss->ss_family) {
    case AF_INET:
        return ntohs(reinterpret_cast<sockaddr_in*>(ss)->sin_port);
    case AF_INET6:
        return ntohs(reinterpret_cast<sockaddr_in6*>(ss)->sin6_port);
    default:
        return -1;
    }
}

static void OSNetworkSystem_accept(JNIEnv* env, jobject, jobject serverFileDescriptor,
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

    int clientFd;
    {
        int intFd = serverFd.get();
        AsynchronousSocketCloseMonitor monitor(intFd);
        clientFd = NET_FAILURE_RETRY(serverFd, accept(intFd, sa, &addrLen));
    }
    if (env->ExceptionOccurred()) {
        return;
    }
    if (clientFd == -1) {
        if (errno == EAGAIN || errno == EWOULDBLOCK) {
            jniThrowSocketTimeoutException(env, errno);
        } else {
            jniThrowSocketException(env, errno);
        }
        return;
    }

    /*
     * For network sockets, put the peer address and port in instance variables.
     * We don't bother to do this for UNIX domain sockets, since most peers are
     * anonymous anyway.
     */
    if (ss.ss_family == AF_INET || ss.ss_family == AF_INET6) {
        jobject remoteAddress = socketAddressToInetAddress(env, &ss);
        if (remoteAddress == NULL) {
            close(clientFd);
            return;
        }
        int remotePort = getSocketAddressPort(&ss);

        static jfieldID addressFid = env->GetFieldID(JniConstants::socketImplClass, "address", "Ljava/net/InetAddress;");
        static jfieldID portFid = env->GetFieldID(JniConstants::socketImplClass, "port", "I");
        env->SetObjectField(newSocket, addressFid, remoteAddress);
        env->SetIntField(newSocket, portFid, remotePort);
    }

    jniSetFileDescriptorOfFD(env, clientFileDescriptor, clientFd);
}




static jint OSNetworkSystem_readDirect(JNIEnv* env, jobject, jobject fileDescriptor, jint address, jint count) {
    NetFd fd(env, fileDescriptor);
    if (fd.isClosed()) {
        return 0;
    }

    jbyte* dst = reinterpret_cast<jbyte*>(static_cast<uintptr_t>(address));
    ssize_t bytesReceived;
    {
        int intFd = fd.get();
        AsynchronousSocketCloseMonitor monitor(intFd);
        bytesReceived = NET_FAILURE_RETRY(fd, read(intFd, dst, count));
    }
    if (env->ExceptionOccurred()) {
        return -1;
    }
    if (bytesReceived == 0) {
        return -1;
    } else if (bytesReceived == -1) {
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

static jint OSNetworkSystem_read(JNIEnv* env, jobject, jobject fileDescriptor, jbyteArray byteArray, jint offset, jint count) {
    ScopedByteArrayRW bytes(env, byteArray);
    if (bytes.get() == NULL) {
        return -1;
    }
    jint address = static_cast<jint>(reinterpret_cast<uintptr_t>(bytes.get() + offset));
    return OSNetworkSystem_readDirect(env, NULL, fileDescriptor, address, count);
}

static jint OSNetworkSystem_recvDirect(JNIEnv* env, jobject, jobject fileDescriptor, jobject packet, jint address, jint offset, jint length, jboolean peek, jboolean connected) {
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

    ssize_t bytesReceived;
    {
        int intFd = fd.get();
        AsynchronousSocketCloseMonitor monitor(intFd);
        bytesReceived = NET_FAILURE_RETRY(fd, recvfrom(intFd, buf, length, flags, from, fromLength));
    }
    if (env->ExceptionOccurred()) {
        return -1;
    }
    if (bytesReceived == -1) {
        if (connected && errno == ECONNREFUSED) {
            jniThrowException(env, "java/net/PortUnreachableException", "");
        } else if (errno == EAGAIN || errno == EWOULDBLOCK) {
            jniThrowSocketTimeoutException(env, errno);
        } else {
            jniThrowSocketException(env, errno);
        }
        return 0;
    }

    static jfieldID addressFid = env->GetFieldID(JniConstants::datagramPacketClass, "address", "Ljava/net/InetAddress;");
    static jfieldID lengthFid = env->GetFieldID(JniConstants::datagramPacketClass, "length", "I");
    static jfieldID portFid = env->GetFieldID(JniConstants::datagramPacketClass, "port", "I");
    if (packet != NULL) {
        env->SetIntField(packet, lengthFid, bytesReceived);
        if (!connected) {
            jobject sender = socketAddressToInetAddress(env, &ss);
            if (sender == NULL) {
                return 0;
            }
            int port = getSocketAddressPort(&ss);
            env->SetObjectField(packet, addressFid, sender);
            env->SetIntField(packet, portFid, port);
        }
    }
    return bytesReceived;
}

static jint OSNetworkSystem_recv(JNIEnv* env, jobject, jobject fd, jobject packet, jbyteArray javaBytes, jint offset, jint length, jboolean peek, jboolean connected) {
    ScopedByteArrayRW bytes(env, javaBytes);
    if (bytes.get() == NULL) {
        return -1;
    }
    uintptr_t address = reinterpret_cast<uintptr_t>(bytes.get());
    return OSNetworkSystem_recvDirect(env, NULL, fd, packet, address, offset, length, peek, connected);
}

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(OSNetworkSystem, accept, "(Ljava/io/FileDescriptor;Ljava/net/SocketImpl;Ljava/io/FileDescriptor;)V"),
    NATIVE_METHOD(OSNetworkSystem, read, "(Ljava/io/FileDescriptor;[BII)I"),
    NATIVE_METHOD(OSNetworkSystem, readDirect, "(Ljava/io/FileDescriptor;II)I"),
    NATIVE_METHOD(OSNetworkSystem, recv, "(Ljava/io/FileDescriptor;Ljava/net/DatagramPacket;[BIIZZ)I"),
    NATIVE_METHOD(OSNetworkSystem, recvDirect, "(Ljava/io/FileDescriptor;Ljava/net/DatagramPacket;IIIZZ)I"),
};

int register_org_apache_harmony_luni_platform_OSNetworkSystem(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "org/apache/harmony/luni/platform/OSNetworkSystem", gMethods, NELEM(gMethods));
}
