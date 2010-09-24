/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.luni.platform;

import dalvik.system.BlockGuard;
import dalvik.system.VMStack;

/**
 * The Platform class gives access to the low-level underlying capabilities of
 * the operating system.
 *
 * The platform is structured into operations on the process heap memory,
 * network subsystem, and file system through different OS components.
 *
 * OS components are 'dangerous' in that they pass through the calls and
 * arguments to the OS with very little checking, and as such may cause fatal
 * exceptions in the runtime. Access to the OS components is restricted to
 * trusted code running on the system classpath.
 *
 * @see IFileSystem
 * @see INetworkSystem
 */
public class Platform {
    // BlockGuard-policy-free threads should have no extra overhead, but for
    // now they do because ThreadLocal lookups will be done on most operations, which
    // should be relatively less than the speed of the operation.
    // TODO: measure & fix if needed.
    public static final IFileSystem FILE_SYSTEM =
            new BlockGuard.WrappedFileSystem(OSFileSystem.getOSFileSystem());

    public static final INetworkSystem NETWORK =
            new BlockGuard.WrappedNetworkSystem(OSNetworkSystem.getOSNetworkSystem());
}
