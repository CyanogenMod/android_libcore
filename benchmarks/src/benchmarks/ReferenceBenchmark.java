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
 * limitations under the License.
 */

package benchmarks;

import com.google.caliper.SimpleBenchmark;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * Benchmark to evaluate the performance of References.
 */
public class ReferenceBenchmark extends SimpleBenchmark {

    private Object object;

    // How fast can references can be allocated?
    public void timeAlloc(int reps) {
        ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
        for (int i = 0; i < reps; i++) {
            new PhantomReference(object, queue);
        }
    }

    // How fast can references can be allocated and manually enqueued?
    public void timeAllocAndEnqueue(int reps) {
        ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
        for (int i = 0; i < reps; i++) {
            (new PhantomReference<Object>(object, queue)).enqueue();
        }
    }

    // How fast can references can be allocated, enqueued, and polled?
    public void timeAllocEnqueueAndPoll(int reps) {
        ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
        for (int i = 0; i < reps; i++) {
            (new PhantomReference<Object>(object, queue)).enqueue();
        }
        for (int i = 0; i < reps; i++) {
            queue.poll();
        }
    }

    // How fast can references can be allocated, enqueued, and removed?
    public void timeAllocEnqueueAndRemove(int reps) {
        ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
        for (int i = 0; i < reps; i++) {
            (new PhantomReference<Object>(object, queue)).enqueue();
        }
        for (int i = 0; i < reps; i++) {
            try {
                queue.remove();
            } catch (InterruptedException ie) {
                i--;
            }
        }
    }
}
