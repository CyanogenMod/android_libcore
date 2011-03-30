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

import java.lang.FinalizerThread;

/**
 * @hide
 */
public final class FinalizerReference<T> extends Reference<T> {
    private static FinalizerReference head = null;

    private FinalizerReference prev;

    private FinalizerReference next;

    public FinalizerReference(T r, ReferenceQueue<? super T> q) {
        super(r, q);
    }

    @Override
    public T get() {
        return (T) pendingNext;
    }

    @Override
    public void clear() {
        pendingNext = null;
    }

    static void add(Object referent) {
        ReferenceQueue<Object> queue = FinalizerThread.queue;
        FinalizerReference<?> reference = new FinalizerReference<Object>(referent, queue);
        synchronized (FinalizerReference.class) {
            reference.prev = null;
            reference.next = head;
            if (head != null) {
                head.prev = reference;
            }
            head = reference;
        }
    }

    public static void remove(FinalizerReference reference) {
        synchronized (FinalizerReference.class) {
            FinalizerReference next = reference.next;
            FinalizerReference prev = reference.prev;
            reference.next = null;
            reference.prev = null;
            if (prev != null) {
                prev.next = next;
            } else {
                head = reference;
            }
            if (next != null) {
                next.prev = prev;
            }
        }
    }
}
