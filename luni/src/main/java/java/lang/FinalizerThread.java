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

package java.lang;

import java.lang.ref.FinalizerReference;
import java.lang.ref.ReferenceQueue;

/**
 * @hide
 */
public final class FinalizerThread extends Thread {
    public static ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
    private static FinalizerThread finalizerThread;
    private static boolean idle;

    static {
        startFinalizer();
    }

    private FinalizerThread() {
        super("Finalizer");
        setDaemon(true);
    }

    public void run() {
        FinalizerReference<Object> reference = null;
        while (true) {
            /*
             * Finalize references until the queue is empty.
             */
            if (reference == null) {
                reference = (FinalizerReference<Object>) queue.poll();
            }
            while (reference != null) {
                doFinalize(reference);
                reference = (FinalizerReference<Object>) queue.poll();
            }

            /*
             * Mark this thread as idle and wait on ReferenceQueue.remove()
             * until awaken by either an enqueued reference or an interruption.
             */
            synchronized (FinalizerThread.class) {
                idle = true;
                FinalizerThread.class.notifyAll();
                if (finalizerThread != this) {
                    return;
                }
            }
            try {
                reference = (FinalizerReference<Object>) queue.remove();
            } catch (InterruptedException ignored) {
            }
            synchronized (FinalizerThread.class) {
                idle = false;
            }
        }
    }

    private void doFinalize(FinalizerReference<Object> reference) {
        FinalizerReference.remove(reference);
        Object obj = reference.get();
        reference.clear();
        try {
            obj.finalize();
        } catch (Throwable ex) {
            // TODO: print a warning
        }
    }

    /**
     * Awakens the finalizer thread if necessary and then wait for it to
     * become idle again. When that happens, all finalizable references enqueued
     * at the time of this method call will have been finalized.
     *
     * TODO: return as soon as the currently-enqueued references are finalized;
     *     this currently waits until the queue is empty. http://b/4193517
     */
    public static synchronized void waitUntilFinalizerIsIdle() throws InterruptedException {
        idle = false;
        finalizerThread.interrupt();
        while (!idle) {
            FinalizerThread.class.wait();
        }
    }

    public static synchronized void startFinalizer() {
        if (finalizerThread != null) {
            throw new IllegalStateException();
        }

        idle = false;
        finalizerThread = new FinalizerThread();
        finalizerThread.start();
    }

    public static synchronized void stopFinalizer() {
        if (finalizerThread == null) {
            throw new IllegalStateException();
        }

        idle = false;
        finalizerThread.interrupt();
        finalizerThread = null;
        try {
            while (!idle) {
                FinalizerThread.class.wait();
            }
        } catch (InterruptedException e) {
            throw new AssertionError();
        }
    }
}
