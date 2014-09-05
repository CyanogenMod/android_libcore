/*
 * Copyright (C) 2006 The Android Open Source Project
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

package dalvik.system;

import java.io.File;

/**
 * Provides hooks for the zygote to call back into the runtime to perform
 * parent or child specific initialization..
 *
 * @hide
 */
public final class ZygoteHooks {
    private long token;

    /**
     * Temporary hack: check time since start time and log if over a fixed threshold.
     *
     */
    private static void checkTime(long startTime, String where) {
        long now = System.nanoTime();
        long msDuration = (now - startTime) / (1000 * 1000);
        if (msDuration > 1000) {
            // If we are taking more than a second, log about it.
            System.logW("Slow operation: " + msDuration + "ms so far, now at " + where);
        }
    }

    /**
     * Called by the zygote prior to every fork. Each call to {@code preFork}
     * is followed by a matching call to {@link #postForkChild(int)} on the child
     * process and {@link #postForkCommon()} on both the parent and the child
     * process. {@code postForkCommon} is called after {@code postForkChild} in
     * the child process.
     */
    public void preFork() {
        long startTime = System.nanoTime();
        Daemons.stop();
        checkTime(startTime, "ZygoteHooks.Daemons.stop");
        waitUntilAllThreadsStopped();
        checkTime(startTime, "ZygoteHooks.waituntilallthreadsstopped");
        token = nativePreFork();
        checkTime(startTime, "ZygoteHooks.Daemons.nativePreFork");
    }

    /**
     * Called by the zygote in the child process after every fork. The debug
     * flags from {@code debugFlags} are applied to the child process.
     */
    public void postForkChild(int debugFlags) {
        nativePostForkChild(token, debugFlags);
    }

    /**
     * Called by the zygote in both the parent and child processes after
     * every fork. In the child process, this method is called after
     * {@code postForkChild}.
     */
    public void postForkCommon() {
        Daemons.start();
    }

    private static native long nativePreFork();
    private static native void nativePostForkChild(long token, int debugFlags);

    /**
     * We must not fork until we're single-threaded again. Wait until /proc shows we're
     * down to just one thread.
     */
    private static void waitUntilAllThreadsStopped() {
        File tasks = new File("/proc/self/task");
        while (tasks.list().length > 1) {
            try {
                // Experimentally, booting and playing about with a stingray, I never saw us
                // go round this loop more than once with a 10ms sleep.
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
