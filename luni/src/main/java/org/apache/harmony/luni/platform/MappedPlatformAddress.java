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

import java.io.IOException;
import java.nio.channels.FileChannel.MapMode;

public class MappedPlatformAddress extends PlatformAddress {
    public static PlatformAddress mmap(int fd, long start, long size, MapMode mode) throws IOException {
        if (size == 0) {
            // You can't mmap(2) a zero-length region.
            return new MappedPlatformAddress(0, 0);
        }
        int address = OSMemory.mmap(fd, start, size, mode);
        return new MappedPlatformAddress(address, size);
    }

    private MappedPlatformAddress(int address, long size) {
        super(address, size);
    }

    public final void mmapLoad() {
        OSMemory.load(osaddr, size);
    }

    public final boolean mmapIsLoaded() {
        return OSMemory.isLoaded(osaddr, size);
    }

    public final void msync() {
        OSMemory.msync(osaddr, size);
    }

    @Override public void free() {
        if (osaddr != 0) {
            OSMemory.munmap(osaddr, size);
            osaddr = 0;
        }
    }

    @Override protected void finalize() throws Throwable {
        free();
    }
}
