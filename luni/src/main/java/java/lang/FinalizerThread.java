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
 *@hide
 */
public final class FinalizerThread extends Thread {
    public static ReferenceQueue queue = new ReferenceQueue();

    private static FinalizerThread thread;

    static {
        startFinalizer();
    }

    private boolean isFinalizing = false;

    public FinalizerThread(String string) {
        super(string);
        setDaemon(true);
    }

    public void run() {
        FinalizerReference reference;
        for (;;) {
            reference = (FinalizerReference)queue.poll();
            if (reference == null) {
                synchronized (FinalizerThread.class) {
                    isFinalizing = false;
                    FinalizerThread.class.notifyAll();
                }
                try {
                    reference = (FinalizerReference)queue.remove();
                } catch (InterruptedException ex) {
                    break;
                }
                synchronized (FinalizerThread.class) {
                    isFinalizing = true;
                }
            }
            doFinalize(reference);
        }
    }


    public boolean isFinalizing() {
        return isFinalizing;
    }

    private void doFinalize(FinalizerReference reference) {
        FinalizerReference.remove(reference);
        Object obj = reference.get();
        reference.clear();
        try {
            obj.finalize();
        } catch (Throwable ex) {}
    }

    public static synchronized void runFinalization() {
        if (thread != null) {
            while (thread.isFinalizing()) {
                try {
                    FinalizerThread.class.wait();
                } catch (InterruptedException ex) {}
            }
        }
    }

    public static synchronized void startFinalizer() {
        if (thread != null) {
            return;
        }
        thread = new FinalizerThread("Finalizer");
        thread.start();
    }

    public static synchronized void stopFinalizer() {
        if (thread == null) {
            return;
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
