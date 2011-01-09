/*
 * Copyright (C) 2009 The Android Open Source Project
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

#ifdef HAVE_ANDROID_OS

#include "ifaddrs-android.h"

#include <arpa/inet.h>
#include <cstring>
#include <errno.h>
#include <net/if.h>
#include <netinet/in.h>
#include <new>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <stdio.h>
#include <linux/netlink.h>
#include <linux/rtnetlink.h>

#include "ScopedFd.h"
#include "UniquePtr.h"

ifaddrs::ifaddrs(ifaddrs* next, sockaddr* ifa_addr, sockaddr* ifa_netmask)
: ifa_next(next), ifa_name(NULL), ifa_flags(0), ifa_addr(ifa_addr), ifa_netmask(ifa_netmask)
{
}

ifaddrs::~ifaddrs() {
    delete ifa_next;
    delete[] ifa_name;
    delete ifa_addr;
    delete ifa_netmask;
}

static bool sendNetlinkMessage(int fd, const void* data, size_t byteCount) {
    ssize_t sentByteCount = TEMP_FAILURE_RETRY(send(fd, data, byteCount, 0));
    return (sentByteCount == static_cast<ssize_t>(byteCount));
}

static ssize_t recvNetlinkMessage(int fd, char* buf, size_t byteCount) {
    return TEMP_FAILURE_RETRY(recv(fd, buf, byteCount, 0));
}

// Returns a pointer to the first byte in the address data (which is
// stored in network byte order).
static uint8_t* sockaddrBytes(int family, sockaddr_storage* ss) {
    if (family == AF_INET) {
        sockaddr_in* ss4 = reinterpret_cast<sockaddr_in*>(ss);
        return reinterpret_cast<uint8_t*>(&ss4->sin_addr);
    } else if (family == AF_INET6) {
        sockaddr_in6* ss6 = reinterpret_cast<sockaddr_in6*>(ss);
        return reinterpret_cast<uint8_t*>(&ss6->sin6_addr);
    }
    return NULL;
}

// Netlink gives us the address family in the header, and the
// sockaddr_in or sockaddr_in6 bytes as the payload. We need to
// stitch the two bits together into the sockaddr that's part of
// our portable interface.
static sockaddr* toSocketAddress(int family, void* data, size_t byteCount) {
    // Set the address proper...
    sockaddr_storage* ss = new sockaddr_storage;
    memset(ss, 0, sizeof(*ss));
    ss->ss_family = family;
    uint8_t* dst = sockaddrBytes(family, ss);
    memcpy(dst, data, byteCount);
    return reinterpret_cast<sockaddr*>(ss);
}

// Netlink gives us the prefix length as a bit count. We need to turn
// that into a BSD-compatible netmask represented by a sockaddr*.
static sockaddr* toNetmask(int family, size_t prefixLength) {
    // ...and work out the netmask from the prefix length.
    sockaddr_storage* ss = new sockaddr_storage;
    memset(ss, 0, sizeof(*ss));
    ss->ss_family = family;
    uint8_t* dst = sockaddrBytes(family, ss);
    memset(dst, 0xff, prefixLength / 8);
    if ((prefixLength % 8) != 0) {
        dst[prefixLength/8] = (0xff << (8 - (prefixLength % 8)));
    }
    return reinterpret_cast<sockaddr*>(ss);
}

// Sadly, we can't keep the interface index for portability with BSD.
// We'll have to keep the name instead, and re-query the index when
// we need it later.
static bool setNameAndFlagsByIndex(ifaddrs* ifa, int interfaceIndex) {
    // Get the name.
    char buf[IFNAMSIZ];
    char* name = if_indextoname(interfaceIndex, buf);
    if (name == NULL) {
        return false;
    }
    ifa->ifa_name = new char[strlen(name) + 1];
    strcpy(ifa->ifa_name, name);

    // Get the flags.
    ScopedFd fd(socket(AF_INET, SOCK_DGRAM, 0));
    if (fd.get() == -1) {
        return false;
    }
    ifreq ifr;
    memset(&ifr, 0, sizeof(ifr));
    strcpy(ifr.ifr_name, name);
    int rc = ioctl(fd.get(), SIOCGIFFLAGS, &ifr);
    if (rc == -1) {
        return false;
    }
    ifa->ifa_flags = ifr.ifr_flags;
    return true;
}

// Source-compatible with the BSD function.
int getifaddrs(ifaddrs** result) {
    // Simplify cleanup for callers.
    *result = NULL;

    // Create a netlink socket.
    ScopedFd fd(socket(AF_NETLINK, SOCK_DGRAM, NETLINK_ROUTE));
    if (fd.get() < 0) {
        return -1;
    }

    // Ask for the address information.
    struct {
        nlmsghdr netlinkHeader;
        ifaddrmsg msg;
    } request;
    memset(&request, 0, sizeof(request));
    request.netlinkHeader.nlmsg_flags = NLM_F_ROOT | NLM_F_REQUEST | NLM_F_MATCH;
    request.netlinkHeader.nlmsg_type = RTM_GETADDR;
    request.netlinkHeader.nlmsg_len = NLMSG_ALIGN(NLMSG_LENGTH(sizeof(request)));
    request.msg.ifa_family = AF_UNSPEC; // All families.
    request.msg.ifa_index = 0; // All interfaces.
    if (!sendNetlinkMessage(fd.get(), &request, request.netlinkHeader.nlmsg_len)) {
        return -1;
    }

    // Read the responses.
    const size_t bufferSize = 65536;
    UniquePtr<char[]> buf(new char[bufferSize]);
    ssize_t bytesRead;
    while ((bytesRead  = recvNetlinkMessage(fd.get(), &buf[0], bufferSize)) > 0) {
        nlmsghdr* hdr = reinterpret_cast<nlmsghdr*>(&buf[0]);
        for (; NLMSG_OK(hdr, (size_t)bytesRead); hdr = NLMSG_NEXT(hdr, bytesRead)) {
            switch (hdr->nlmsg_type) {
            case NLMSG_DONE:
                return 0;
            case NLMSG_ERROR:
                return -1;
            case RTM_NEWADDR:
                {
                    // A given RTM_NEWADDR payload may contain multiple addresses. The while loop
                    // below iterates through them. These locals contain the best address we've
                    // seen so far.
                    int ifa_index = -1;
                    UniquePtr<sockaddr> ifa_addr;
                    UniquePtr<sockaddr> ifa_netmask;

                    ifaddrmsg* address = reinterpret_cast<ifaddrmsg*>(NLMSG_DATA(hdr));
                    rtattr* rta = IFA_RTA(address);
                    size_t ifaPayloadLength = IFA_PAYLOAD(hdr);
                    while (RTA_OK(rta, ifaPayloadLength)) {
                        // We can't just use IFA_ADDRESS because it's the destination address for
                        // a point-to-point interface; we can't just use IFA_LOCAL because we don't
                        // always have it. That is: we want IFA_LOCAL if we get it, but IFA_ADDRESS
                        // otherwise. We take advantage of the fact that the kernel returns
                        // IFA_LOCAL (if available) second.
                        if (rta->rta_type == IFA_LOCAL || rta->rta_type == IFA_ADDRESS) {
                            int family = address->ifa_family;
                            if (family == AF_INET || family == AF_INET6) {
                                ifa_index = address->ifa_index;
                                ifa_addr.reset(toSocketAddress(family, RTA_DATA(rta), RTA_PAYLOAD(rta)));
                                ifa_netmask.reset(toNetmask(family, address->ifa_prefixlen));
                            }
                        }
                        rta = RTA_NEXT(rta, ifaPayloadLength);
                    }

                    // Did we get a usable address? If so, thread it on our list.
                    if (ifa_index != -1) {
                        *result = new ifaddrs(*result, ifa_addr.release(), ifa_netmask.release());
                        if (!setNameAndFlagsByIndex(*result, ifa_index)) {
                            return -1;
                        }
                    }
                }
                break;
            }
        }
    }
    // We only get here if recv fails before we see a NLMSG_DONE.
    return -1;
}

// Source-compatible with the BSD function.
void freeifaddrs(ifaddrs* addresses) {
    delete addresses;
}

#endif
