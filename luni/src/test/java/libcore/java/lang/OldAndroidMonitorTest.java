/*
 * Copyright (C) 2007 The Android Open Source Project
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

package libcore.java.lang;

import junit.framework.TestCase;

public class OldAndroidMonitorTest extends TestCase {

    public void testWaitArgumentsTest() throws Exception {
            /* Try some valid arguments.  These should all
             * return very quickly.
             */
            try {
                synchronized (this) {
                    /* millisecond version */
                    wait(1);
                    wait(10);

                    /* millisecond + nanosecond version */
                    wait(0, 1);
                    wait(0, 999999);
                    wait(1, 1);
                    wait(1, 999999);
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException("good Object.wait() interrupted",
                        ex);
            } catch (Exception ex) {
                throw new RuntimeException("Unexpected exception when calling" +
                        "Object.wait() with good arguments", ex);
            }

            /* Try some invalid arguments.
             */
            boolean sawException = false;
            try {
                synchronized (this) {
                    wait(-1);
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException("bad Object.wait() interrupted", ex);
            } catch (IllegalArgumentException ex) {
                sawException = true;
            } catch (Exception ex) {
                throw new RuntimeException("Unexpected exception when calling" +
                        "Object.wait() with bad arguments", ex);
            }
            if (!sawException) {
                throw new RuntimeException("bad call to Object.wait() should " +
                        "have thrown IllegalArgumentException");
            }

            sawException = false;
            try {
                synchronized (this) {
                    wait(0, -1);
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException("bad Object.wait() interrupted", ex);
            } catch (IllegalArgumentException ex) {
                sawException = true;
            } catch (Exception ex) {
                throw new RuntimeException("Unexpected exception when calling" +
                        "Object.wait() with bad arguments", ex);
            }
            if (!sawException) {
                throw new RuntimeException("bad call to Object.wait() should " +
                        "have thrown IllegalArgumentException");
            }

            sawException = false;
            try {
                synchronized (this) {
                    /* The legal range of nanos is 0-999999. */
                    wait(0, 1000000);
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException("bad Object.wait() interrupted", ex);
            } catch (IllegalArgumentException ex) {
                sawException = true;
            } catch (Exception ex) {
                throw new RuntimeException("Unexpected exception when calling" +
                        "Object.wait() with bad arguments", ex);
            }
            if (!sawException) {
                throw new RuntimeException("bad call to Object.wait() should " +
                        "have thrown IllegalArgumentException");
            }
    }

    private class Interrupter extends Thread {
            private final Waiter waiter;

            Interrupter(String name, Waiter waiter) {
                super(name);
                this.waiter = waiter;
            }

            public void run() {
                try {
                    run_inner();
                } catch (Throwable t) {
                    OldAndroidMonitorTest.errorException = t;
                    OldAndroidMonitorTest.testThread.interrupt();
                }
            }

            private void run_inner() {
                // System.out.println("InterruptTest: starting waiter");
                waiter.start();

                try {
                    Thread.currentThread().sleep(500);
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Test sleep interrupted.", ex);
                }

                /* Waiter is spinning, and its monitor should still be thin.
                 */
                // System.out.println("Test interrupting waiter");
                waiter.interrupt();
                waiter.spin = false;

                for (int i = 0; i < 3; i++) {
                    /* Wait for the waiter to start waiting.
                     */
                    synchronized (waiter.interrupterLock) {
                        try {
                            waiter.interrupterLock.wait();
                        } catch (InterruptedException ex) {
                            throw new RuntimeException("Test wait interrupted.", ex);
                        }
                    }

                    /* Before interrupting, grab the waiter lock, which
                     * guarantees that the waiter is already sitting in wait().
                     */
                    synchronized (waiter) {
                        //System.out.println("Test interrupting waiter (" + i + ")");
                        waiter.interrupt();
                    }
                }

                // System.out.println("Test waiting for waiter to die.");
                try {
                    waiter.join();
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Test join interrupted.", ex);
                }
                // System.out.println("InterruptTest done.");
            }
        }

    private class Waiter extends Thread {
            Object interrupterLock = new Object();
            volatile boolean spin = true;

            Waiter(String name) {
                super(name);
            }

            public void run() {
                try {
                    run_inner();
                } catch (Throwable t) {
                    OldAndroidMonitorTest.errorException = t;
                    OldAndroidMonitorTest.testThread.interrupt();
                }
            }

            void run_inner() {
                // System.out.println("Waiter spinning");
                while (spin) {
                    // We're going to get interrupted while we spin.
                }

                if (interrupted()) {
                    // System.out.println("Waiter done spinning; interrupted.");
                } else {
                    throw new RuntimeException("Thread not interrupted " +
                                               "during spin");
                }

                synchronized (this) {
                    boolean sawEx = false;

                    try {
                        synchronized (interrupterLock) {
                            interrupterLock.notify();
                        }
                        // System.out.println("Waiter calling wait()");
                        this.wait();
                    } catch (InterruptedException ex) {
                        sawEx = true;
                        // System.out.println("wait(): Waiter caught " + ex);
                    }
                    // System.out.println("wait() finished");

                    if (!sawEx) {
                        throw new RuntimeException("Thread not interrupted " +
                                                   "during wait()");
                    }
                }
                synchronized (this) {
                    boolean sawEx = false;

                    try {
                        synchronized (interrupterLock) {
                            interrupterLock.notify();
                        }
                        // System.out.println("Waiter calling wait(1000)");
                        this.wait(1000);
                    } catch (InterruptedException ex) {
                        sawEx = true;
                        // System.out.println("wait(1000): Waiter caught " + ex);
                    }
                    // System.out.println("wait(1000) finished");

                    if (!sawEx) {
                        throw new RuntimeException("Thread not interrupted " +
                                                   "during wait(1000)");
                    }
                }
                synchronized (this) {
                    boolean sawEx = false;

                    try {
                        synchronized (interrupterLock) {
                            interrupterLock.notify();
                        }
                        // System.out.println("Waiter calling wait(1000, 5000)");
                        this.wait(1000, 5000);
                    } catch (InterruptedException ex) {
                        sawEx = true;
                        // System.out.println("wait(1000, 5000): Waiter caught " + ex);
                    }
                    // System.out.println("wait(1000, 5000) finished");

                    if (!sawEx) {
                        throw new RuntimeException("Thread not interrupted " +
                                                   "during wait(1000, 5000)");
                    }
                }

               //  System.out.println("Waiter returning");
            }
        }

    private static Throwable errorException;
    private static Thread testThread;

    // TODO: Flaky test. Add back MediumTest annotation once fixed
    public void testInterruptTest() throws Exception {


            testThread = Thread.currentThread();
            errorException = null;

            Waiter waiter = new Waiter("InterruptTest Waiter");
            Interrupter interrupter =
                    new Interrupter("InterruptTest Interrupter", waiter);
            interrupter.start();

            try {
                interrupter.join();
                waiter.join();
            } catch (InterruptedException ex) {
                throw new RuntimeException("Test join interrupted.", ex);
            }

            if (errorException != null) {
                throw new RuntimeException("InterruptTest failed",
                                           errorException);
            }




    }
}
