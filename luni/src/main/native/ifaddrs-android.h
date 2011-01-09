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

#ifndef IFADDRS_ANDROID_H_included
#define IFADDRS_ANDROID_H_included

struct sockaddr;

// Android (bionic) doesn't have getifaddrs(3)/freeifaddrs(3).
// We fake it here, so java_net_NetworkInterface.cpp can use that API.
// This code should move into bionic, though.

// Source-compatible subset of the BSD struct.
struct ifaddrs {
    // Pointer to next struct in list, or NULL at end.
    ifaddrs* ifa_next;

    // Interface name.
    char* ifa_name;

    // Interface flags.
    unsigned int ifa_flags;

    // Interface network address.
    sockaddr* ifa_addr;

    // Interface netmask.
    sockaddr* ifa_netmask;

    ifaddrs(ifaddrs* next, sockaddr* ifa_addr, sockaddr* ifa_netmask);

    ~ifaddrs();

private:
    // Disallow copy and assignment.
    ifaddrs(const ifaddrs&);
    void operator=(const ifaddrs&);
};

int getifaddrs(ifaddrs** result);
void freeifaddrs(ifaddrs* addresses);

#endif  // IFADDRS_ANDROID_H_included
