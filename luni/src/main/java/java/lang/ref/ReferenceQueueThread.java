/*
 * Copyright (C) 2011 The Android Open Source Project
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

package java.lang.ref;

/**
 * @hide
 */
public final class ReferenceQueueThread extends Thread {
    private static ReferenceQueueThread thread = null;

    public ReferenceQueueThread() {
        super("ReferenceQueue");
        setDaemon(true);
    }

    /**
     * Moves each element from the pending list to the reference queue
     * list.  The pendingNext field is owned by the garbage collector
     * so no synchronization is required to perform the unlinking.
     */
    private static void doEnqueue(Reference list) {
        while (list != null) {
            Reference reference;
            if (list == list.pendingNext) {
                reference = list;
                reference.pendingNext = null;
                list = null;
            } else {
                reference = list.pendingNext;
                list.pendingNext = reference.pendingNext;
                reference.pendingNext = null;
            }
            reference.enqueueInternal();
        }
    }

    public void run() {
        for (;;) {
            Reference list;
            try {
                synchronized (ReferenceQueue.class) {
                    while (ReferenceQueue.unenqueued == null) {
                        ReferenceQueue.class.wait();
                    }
                    list = ReferenceQueue.unenqueued;
                    ReferenceQueue.unenqueued = null;
                }
            } catch (InterruptedException ex) {
                break;
            }
            doEnqueue(list);
        }
    }

    public static synchronized void startReferenceQueue() {
        if (thread != null) {
            throw new IllegalStateException("already started");
        }
        thread = new ReferenceQueueThread();
        thread.start();
    }

    public static synchronized void stopReferenceQueue() {
        if (thread == null) {
            throw new IllegalStateException("not started");
        }
        thread.interrupt();
        for (;;) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                continue;
            }
            break;
        }
        thread = null;
    }
}
