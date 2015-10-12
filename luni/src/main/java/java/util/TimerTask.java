/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package java.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A task to be scheduled in a {@link Timer}.
 *
 * Is it assumed that each {@link TimerTask} is at most once in a {@link Timer} and is used in
 * at most one scheduling call.
 */
public abstract class TimerTask implements Runnable {
    private boolean cancelled = false;
    private long scheduledExecutionTime = 0;
    private boolean scheduled = false;
    // Lock for protecting access to members.
    private final Object lock = new Object();

    protected TimerTask() {

    }

    /** Inherited from {@link Runnable} */
    public abstract void run();

    /**
     * Cancels this task and guarantees that this task will never run again.
     *
     * <p>If this task is currently running, the current run() will be allowed to complete.
     * {@code cancel()} can be called more than once and it has no effect after the first call.
     *
     * <p>Returns {@code true} iff. at least one run of the task was cancelled. Note that if the
     * given task wasn’t scheduled, there’s nothing to cancel (by definition) so this method must
     * return {@code false} in that case.
     */
    public boolean cancel() {
        synchronized(lock) {
            if (cancelled) {
                return false;
            }
            cancelled = true;
            return scheduled;
        }
    }

    /**
     * Returns the time at which the most recent execution of this task was <em>scheduled</em>
     * to occur.
     *
     * <p>Because task execution can occur later than scheduled this method can be used within the
     * {@link #run()} method to calculate the difference between the scheduled and actual execution
     * time.
     *
     * <p>The return value is undefined for tasks that have never been run.
     */
    public long scheduledExecutionTime() {
        synchronized (lock) {
            return scheduledExecutionTime;
        }
    }

    boolean isCancelled() {
        synchronized(lock) {
            return cancelled;
        }
    }

    void setScheduledExecutionTime(long timeMillis) {
        synchronized (lock) {
            scheduledExecutionTime = timeMillis;
        }
    }

    void setScheduled(boolean scheduled) {
        synchronized (lock) {
            this.scheduled = scheduled;
        }
    }

    void updateScheduledExecutionTimeAndRun(
            long scheduledExecutionTime,
            boolean hasAnotherFutureExecutionScheduled) {
        synchronized(lock) {
            if (isCancelled()) {
                return;
            }
            setScheduledExecutionTime(scheduledExecutionTime);
            setScheduled(hasAnotherFutureExecutionScheduled);
        }
        run();
    }
}
