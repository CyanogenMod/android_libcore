/*
 * Copyright (C) 2006 The Android Open Source Project
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

#define LOG_TAG "InetAddress"

#include "JNIHelp.h"
#include "JniConstants.h"
#include "LocalArray.h"
#include "NetworkUtilities.h"
#include "ScopedLocalRef.h"
#include "ScopedUtfChars.h"
#include "UniquePtr.h"
#include "jni.h"
#include "utils/Log.h"

#include <arpa/inet.h>
#include <errno.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>

struct addrinfo_deleter {
    void operator()(addrinfo* p) const {
        if (p != NULL) { // bionic's freeaddrinfo(3) crashes when passed NULL.
            freeaddrinfo(p);
        }
    }
};

static jobjectArray InetAddress_getaddrinfo(JNIEnv* env, jclass, jstring javaName) {
    ScopedUtfChars name(env, javaName);
    if (name.c_str() == NULL) {
        return NULL;
    }

    addrinfo hints;
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;
    hints.ai_flags = AI_ADDRCONFIG;
    /*
     * If we don't specify a socket type, every address will appear twice, once
     * for SOCK_STREAM and one for SOCK_DGRAM. Since we do not return the family
     * anyway, just pick one.
     */
    hints.ai_socktype = SOCK_STREAM;

    addrinfo* addressList = NULL;
    int result = getaddrinfo(name.c_str(), NULL, &hints, &addressList);
    UniquePtr<addrinfo, addrinfo_deleter> addressListDeleter(addressList);
    if (result == 0 && addressList) {
        // Count results so we know how to size the output array.
        int addressCount = 0;
        for (addrinfo* ai = addressList; ai != NULL; ai = ai->ai_next) {
            if (ai->ai_family == AF_INET || ai->ai_family == AF_INET6) {
                ++addressCount;
            }
        }

        // Prepare output array.
        jobjectArray result = env->NewObjectArray(addressCount, JniConstants::byteArrayClass, NULL);
        if (result == NULL) {
            return NULL;
        }

        // Examine returned addresses one by one, save them in the output array.
        int index = 0;
        for (addrinfo* ai = addressList; ai != NULL; ai = ai->ai_next) {
            if (ai->ai_family != AF_INET && ai->ai_family != AF_INET6) {
                // Unknown address family. Skip this address.
                continue;
            }

            // Convert each IP address into a Java byte array.
            sockaddr_storage* address = reinterpret_cast<sockaddr_storage*>(ai->ai_addr);
            ScopedLocalRef<jbyteArray> byteArray(env, socketAddressToByteArray(env, address));
            if (byteArray.get() == NULL) {
                return NULL;
            }
            env->SetObjectArrayElement(result, index, byteArray.get());
            ++index;
        }

        return result;
    } else if (result == EAI_SYSTEM && errno == EACCES) {
        /* No permission to use network */
        jniThrowException(env, "java/lang/SecurityException",
                "Permission denied (maybe missing INTERNET permission)");
        return NULL;
    } else {
        jniThrowExceptionFmt(env, "java/net/UnknownHostException",
                "Unable to resolve host \"%s\": %s", name.c_str(), gai_strerror(result));
        return NULL;
    }
}

static jbyteArray InetAddress_ipStringToByteArray(JNIEnv* env, jobject, jstring javaString) {
    // Convert the String to UTF-8 bytes.
    ScopedUtfChars chars(env, javaString);
    if (chars.c_str() == NULL) {
        return NULL;
    }
    size_t byteCount = chars.size();
    LocalArray<INET6_ADDRSTRLEN> bytes(byteCount + 1);
    char* ipString = &bytes[0];
    strcpy(ipString, chars.c_str());

    // Accept IPv6 addresses (only) in square brackets for compatibility.
    if (ipString[0] == '[' && ipString[byteCount - 1] == ']' && strchr(ipString, ':') != NULL) {
        memmove(ipString, ipString + 1, byteCount - 2);
        ipString[byteCount - 2] = '\0';
    }

    addrinfo hints;
    memset(&hints, 0, sizeof(hints));
    hints.ai_flags = AI_NUMERICHOST;

    sockaddr_storage ss;
    memset(&ss, 0, sizeof(ss));

    addrinfo* res = NULL;
    int ret = getaddrinfo(ipString, NULL, &hints, &res);
    UniquePtr<addrinfo, addrinfo_deleter> addressListDeleter(res);
    if (ret == 0 && res) {
        // Convert IPv4-mapped addresses to IPv4 addresses.
        // The RI states "Java will never return an IPv4-mapped address".
        sockaddr_in6* sin6 = reinterpret_cast<sockaddr_in6*>(res->ai_addr);
        if (res->ai_family == AF_INET6 && IN6_IS_ADDR_V4MAPPED(&sin6->sin6_addr)) {
            sockaddr_in* sin = reinterpret_cast<sockaddr_in*>(&ss);
            sin->sin_family = AF_INET;
            sin->sin_port = sin6->sin6_port;
            memcpy(&sin->sin_addr.s_addr, &sin6->sin6_addr.s6_addr[12], 4);
            return socketAddressToByteArray(env, &ss);
        } else {
            return socketAddressToByteArray(env, reinterpret_cast<sockaddr_storage*>(res->ai_addr));
        }
    } else {
        // For backwards compatibility, deal with address formats that
        // getaddrinfo does not support. For example, 1.2.3, 1.3, and even 3 are
        // valid IPv4 addresses according to the Java API. If getaddrinfo fails,
        // try to use inet_aton.
        sockaddr_in* sin = reinterpret_cast<sockaddr_in*>(&ss);
        if (inet_aton(ipString, &sin->sin_addr)) {
            sin->sin_family = AF_INET;
            sin->sin_port = 0;
            return socketAddressToByteArray(env, &ss);
        }
    }
    return NULL;
}

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(InetAddress, getaddrinfo, "(Ljava/lang/String;)[[B"),
    NATIVE_METHOD(InetAddress, ipStringToByteArray, "(Ljava/lang/String;)[B"),
};
int register_java_net_InetAddress(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "java/net/InetAddress", gMethods, NELEM(gMethods));
}
