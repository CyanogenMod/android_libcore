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

import java.util.concurrent.atomic.AtomicBoolean;
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

    /**
     * http://b/issue?id=2136462
     */
    public void testBackFromTheDead() throws Exception {
        try {
            new ConstructionFails();
        } catch (AssertionError expected) {
        }

        induceFinalization();

        if (ConstructionFails.INSTANCE != null) {
            fail("finalize() called, even though constructor failed!");
        }
    }

    private void induceFinalization() throws Exception {
        System.gc();
        System.runFinalization();
    }

    static class ConstructionFails {
        private static ConstructionFails INSTANCE;

        ConstructionFails() {
            throw new AssertionError();
        }

        @Override protected void finalize() throws Throwable {
            INSTANCE = this;
            new AssertionError("finalize() called, even though constructor failed!")
                    .printStackTrace();
        }
    }
}
