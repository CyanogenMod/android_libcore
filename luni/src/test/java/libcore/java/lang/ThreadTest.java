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

package libcore.java.lang;

import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;

public final class ThreadTest extends TestCase {

    public void testLeakingStartedThreads() {
        final AtomicInteger finalizedThreadsCount = new AtomicInteger();
        for (int i = 0; true; i++) {
            try {
                newThread(finalizedThreadsCount, 1024 << i).start();
            } catch (OutOfMemoryError expected) {
                break;
            }
        }
        System.runFinalization();
        assertTrue("Started threads were never finalized!", finalizedThreadsCount.get() > 0);
    }

    public void testLeakingUnstartedThreads() {
        final AtomicInteger finalizedThreadsCount = new AtomicInteger();
        for (int i = 0; true; i++) {
            try {
                newThread(finalizedThreadsCount, 1024 << i);
            } catch (OutOfMemoryError expected) {
                break;
            }
        }
        System.runFinalization();
        assertTrue("Unstarted threads were never finalized!", finalizedThreadsCount.get() > 0);
    }

    private Thread newThread(final AtomicInteger finalizedThreadsCount, final int size) {
        return new Thread() {
            byte[] memoryPressure = new byte[size];
            @Override protected void finalize() throws Throwable {
                super.finalize();
                finalizedThreadsCount.incrementAndGet();
            }
        };
    }
}
