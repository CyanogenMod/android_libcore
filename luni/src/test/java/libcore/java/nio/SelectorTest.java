/*
 * Copyright (C) 2010 The Android Open Source Project
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

package libcore.java.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.NoConnectionPendingException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;

public class SelectorTest extends TestCase {

    private Selector selector;
    private ServerSocketChannel ssc;

    protected void setUp() throws Exception {
        super.setUp();
        ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.socket().bind(null);
        selector = Selector.open();
    }

    protected void tearDown() throws Exception {
        try {
            ssc.close();
        } catch (Exception e) {
            // do nothing
        }
        try {
            selector.close();
        } catch (Exception e) {
            // do nothing
        }
        super.tearDown();
    }

    // http://code.google.com/p/android/issues/detail?id=6309
    public void test_connectFinish_fails() throws Exception {
        final SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_CONNECT);
        final Boolean[] fail = new Boolean[1];
        new Thread() {
            public void run() {
                try {
                    while (selector.isOpen()) {
                        if (selector.select() != 0) {
                            for (SelectionKey key : selector.selectedKeys()) {
                                if (key.isValid() && key.isConnectable()) {
                                    try {
                                        channel.finishConnect();
                                        synchronized (fail) {
                                            fail[0] = Boolean.FALSE;
                                            fail.notify();
                                        }
                                    } catch (NoConnectionPendingException _) {
                                        synchronized (fail) {
                                            fail[0] = Boolean.TRUE;
                                            fail.notify();
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception _) {}
            }
        }.start();

        final int WAIT_TIME_MS = 100;
        Thread.sleep(WAIT_TIME_MS);
        channel.connect(ssc.socket().getLocalSocketAddress());
        long time = System.currentTimeMillis();
        synchronized (fail) {
            while (System.currentTimeMillis() - time < WAIT_TIME_MS || fail[0] == null) {
                fail.wait(WAIT_TIME_MS);
            }
        }
        if (fail[0] == null) {
            fail("test does not work");
        } else if (fail[0].booleanValue()) {
            fail();
        }
    }

    // http://code.google.com/p/android/issues/detail?id=15388
    public void testInterrupted() throws IOException {
        Thread.currentThread().interrupt();
        int count = selector.select();
        assertEquals(0, count);
    }

    public void testManyWakeupCallsTriggerOnlyOneWakeup() throws Exception {
        selector.wakeup();
        selector.wakeup();
        selector.wakeup();
        selector.select();

        // create a latch that will reach 0 when select returns
        final CountDownLatch selectReturned = new CountDownLatch(1);
        Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    selector.select();
                    selectReturned.countDown();
                } catch (IOException ignored) {
                }
            }
        });
        thread.start();

        // select doesn't ever return, so await() times out and returns false
        assertFalse(selectReturned.await(500, TimeUnit.MILLISECONDS));
    }
}
