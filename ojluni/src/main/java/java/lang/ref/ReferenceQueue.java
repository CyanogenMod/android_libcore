/*
 * Copyright (c) 1997, 2005, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang.ref;

/**
 * Reference queues, to which registered reference objects are appended by the
 * garbage collector after the appropriate reachability changes are detected.
 *
 * @author   Mark Reinhold
 * @since    1.2
 */

public class ReferenceQueue<T> {

    /**
     * Constructs a new reference-object queue.
     */
    public ReferenceQueue() { }

    /* ----- BEGIN android -----
    private static class Null extends ReferenceQueue {
        boolean enqueue(Reference r) {
            return false;
        }
    }

    static ReferenceQueue NULL = new Null();
    static ReferenceQueue ENQUEUED = new Null();

    static private class Lock { };
    private Lock lock = new Lock();
    ----- END android ----- */

    private volatile Reference<? extends T> head = null;

    // ----- BEGIN android -----
    // Needed to make it a queue (OpenJdk impl behaves like a stack).
    private Reference<? extends T> tail;
    // ----- END android -----

    private long queueLength = 0;

    boolean enqueue(Reference<? extends T> r) { /* Called only by Reference class */
        /* ----- BEGIN android -----
        synchronized (r) {
            if (r.queue == ENQUEUED) return false;
            synchronized (lock) {
                r.queue = ENQUEUED;
                r.next = (head == null) ? r : head;
                head = r;
                queueLength++;
                if (r instanceof FinalReference) {
                    sun.misc.VM.addFinalRefCount(1);
                }
                lock.notifyAll();
                return true;
            }
        }*/
        /* Caller must hold r lock */
        synchronized (this) {
            if (tail == null) {
                head = r;
            } else {
                tail.queueNext = r;
            }
            tail = r;
            tail.queueNext = r;
            queueLength++;
            this.notifyAll();
            return true;
        }
        // ----- END android -----
    }

    private Reference<? extends T> reallyPoll() {       /* Must hold lock */
        /* ----- BEGIN android -----
        if (head != null) {
            Reference<? extends T> r = head;
            head = (r.next == r) ? null : r.next;
            r.queue = NULL;
            r.next = r;
            queueLength--;
            if (r instanceof FinalReference) {
                sun.misc.VM.addFinalRefCount(-1);
            }
            return r;
        }
        */
        if (head != null) {
            Reference<? extends T> r = head;
            if (head == tail) {
                tail = null;
                head = null;
            } else {
                head = head.queueNext;
            }
            r.queueNext = null;
            queueLength--;
            return r;
        }
        // ----- END android -----
        return null;
    }

    /**
     * Polls this queue to see if a reference object is available.  If one is
     * available without further delay then it is removed from the queue and
     * returned.  Otherwise this method immediately returns <tt>null</tt>.
     *
     * @return  A reference object, if one was immediately available,
     *          otherwise <code>null</code>
     */
    public Reference<? extends T> poll() {
        if (head == null)
            return null;

        /* ----- BEGIN android -----
        synchronized (lock) { */
        synchronized (this) {
        // ----- END android -----
            return reallyPoll();
        }
    }

    /**
     * Removes the next reference object in this queue, blocking until either
     * one becomes available or the given timeout period expires.
     *
     * <p> This method does not offer real-time guarantees: It schedules the
     * timeout as if by invoking the {@link Object#wait(long)} method.
     *
     * @param  timeout  If positive, block for up to <code>timeout</code>
     *                  milliseconds while waiting for a reference to be
     *                  added to this queue.  If zero, block indefinitely.
     *
     * @return  A reference object, if one was available within the specified
     *          timeout period, otherwise <code>null</code>
     *
     * @throws  IllegalArgumentException
     *          If the value of the timeout argument is negative
     *
     * @throws  InterruptedException
     *          If the timeout wait is interrupted
     */
    public Reference<? extends T> remove(long timeout)
        throws IllegalArgumentException, InterruptedException
    {
        if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout value");
        }
        /* ----- BEGIN android -----
        synchronized (lock) { */
        synchronized (this) {
        // ----- END android -----
            Reference<? extends T> r = reallyPoll();
            if (r != null) return r;
            for (;;) {
                /* ----- BEGIN android -----
                lock.wait(timeout);*/
                this.wait(timeout);
                // ----- END android -----
                r = reallyPoll();
                if (r != null) return r;
                if (timeout != 0) return null;
            }
        }
    }

    /**
     * Removes the next reference object in this queue, blocking until one
     * becomes available.
     *
     * @return A reference object, blocking until one becomes available
     * @throws  InterruptedException  If the wait is interrupted
     */
    public Reference<? extends T> remove() throws InterruptedException {
        return remove(0);
    }

    // ----- BEGIN android -----
    /** @hide */
    public static Reference<?> unenqueued = null;

    static void add(Reference<?> list) {
        synchronized (ReferenceQueue.class) {
            if (unenqueued == null) {
                unenqueued = list;
            } else {
                // Find the last element in unenqueued.
                Reference<?> last = unenqueued;
                while (last.pendingNext != unenqueued) {
                  last = last.pendingNext;
                }
                // Add our list to the end. Update the pendingNext to point back to enqueued.
                last.pendingNext = list;
                last = list;
                while (last.pendingNext != list) {
                    last = last.pendingNext;
                }
                last.pendingNext = unenqueued;
            }
            ReferenceQueue.class.notifyAll();
        }
    }
    // ----- END android -----
}