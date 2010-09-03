/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.luni.platform;

public class MallocedPlatformAddress extends PlatformAddress {
    public static PlatformAddress malloc(int byteCount) {
        return new MallocedPlatformAddress(OSMemory.malloc(byteCount), byteCount);
    }

    private MallocedPlatformAddress(int address, long size) {
        super(address, size);
    }

    @Override public void free() {
        if (osaddr != 0) {
            OSMemory.free(osaddr);
            osaddr = 0;
        }
    }

    @Override protected void finalize() throws Throwable {
        free();
    }
}
