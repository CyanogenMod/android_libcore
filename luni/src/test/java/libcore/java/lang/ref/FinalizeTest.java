/*
 * Copyright (C) 2011 Google Inc.
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

package libcore.java.lang.ref;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;

public final class FinalizeTest extends TestCase {

    public void testFinalizeIsCalled() throws Exception {
        AtomicBoolean finalized = new AtomicBoolean();
        createFinalizableObject(finalized);

        induceFinalization();
        if (!finalized.get()) {
            fail();
        }
    }

    /**
     * Prevent live-precise bugs from interfering with analysis of what is
     * reachable. Do not inline this method; otherwise tests may fail on VMs
     * that are not live-precise. http://b/4191345
     */
    private void createFinalizableObject(final AtomicBoolean finalized) {
        new X() {
            @Override protected void finalize() throws Throwable {
                super.finalize();
                finalized.set(true);
            }
        };
    }

    static class X {}

    // http://b/issue?id=2136462
    public void testBackFromTheDead() throws Exception {
        try {
            new ConstructionFails();
        } catch (AssertionError expected) {
        }

        induceFinalization();
        assertTrue("object whose constructor threw was not finalized", ConstructionFails.finalized);
    }

    private void induceFinalization() throws Exception {
        System.gc();
        enqueueReferences();
        System.runFinalization();
    }

    /**
     * Hack. We don't have a programmatic way to wait for the reference queue
     * daemon to move references to the appropriate queues.
     */
    private void enqueueReferences() throws InterruptedException {
        Thread.sleep(100);
    }

    static class ConstructionFails {
        private static boolean finalized;

        ConstructionFails() {
            throw new AssertionError();
        }

        @Override protected void finalize() throws Throwable {
            finalized = true;
        }
    }

    /**
     * The finalizer watch dog exits the VM if any object takes more than 10 s
     * to finalize. Check that objects near that limit are okay.
     */
    public void testWatchdogDoesNotFailForObjectsThatAreNearTheDeadline() throws Exception {
        CountDownLatch latch = new CountDownLatch(5);
        createSlowFinalizer(   1, latch);
        createSlowFinalizer(1000, latch);
        createSlowFinalizer(2000, latch);
        createSlowFinalizer(4000, latch);
        createSlowFinalizer(8000, latch);
        induceFinalization();
        latch.await();
    }

    public void createSlowFinalizer(final long millis, final CountDownLatch latch) {
        new Object() {
            @Override protected void finalize() throws Throwable {
                System.out.println("finalize sleeping " + millis + " ms");
                Thread.sleep(millis);
                latch.countDown();
            }
        };
    }

    /**
     * Make sure that System.runFinalization() returns even if the finalization
     * queue is never completely empty. http://b/4193517
     */
    public void testSystemRunFinalizationReturnsEvenIfQueueIsNonEmpty() throws Exception {
        AtomicInteger count = new AtomicInteger();
        AtomicBoolean keepGoing = new AtomicBoolean(true);
        createChainedFinalizer(count, keepGoing);
        induceFinalization();
        keepGoing.set(false);
        assertTrue(count.get() > 0);
    }

    public void createChainedFinalizer(final AtomicInteger counter, final AtomicBoolean keepGoing) {
        new Object() {
            @Override protected void finalize() throws Throwable {
                int count = counter.incrementAndGet();
                System.out.println(count);
                if (keepGoing.get()) {
                    createChainedFinalizer(counter, keepGoing); // recursive!
                }
                System.gc();
                enqueueReferences();
            }
        };
    }
}
