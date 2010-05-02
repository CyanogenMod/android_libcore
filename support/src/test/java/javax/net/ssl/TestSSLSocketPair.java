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

package javax.net.ssl;

/**
 * TestSSLSocketPair is a convenience class for other tests that want
 * a pair of connected and handshaked client and server SSLSockets for
 * testing.
 */
public final class TestSSLSocketPair {
    public final TestSSLContext c;
    public final SSLSocket server;
    public final SSLSocket client;

    private TestSSLSocketPair (TestSSLContext c,
                               SSLSocket server,
                               SSLSocket client) {
        this.c = c;
        this.server = server;
        this.client = client;
    }

    /**
     * based on test_SSLSocket_startHandshake_workaround, should
     * be written to non-workaround form when possible
     */
    public static TestSSLSocketPair create_workaround () {
        TestSSLContext c = TestSSLContext.create();
        SSLSocket[] sockets = connect_workaround(c, null);
        return new TestSSLSocketPair(c, sockets[0], sockets[1]);
    }

    /**
     * Create a new connected server/client socket pair within a
     * existing SSLContext. Optional clientCipherSuites allows
     * forcing new SSLSession to test SSLSessionContext caching
     */
    public static SSLSocket[] connect_workaround (final TestSSLContext c,
                                                  String[] clientCipherSuites) {
        try {
            final SSLSocket[] server = new SSLSocket[1];
            Thread thread = new Thread(new Runnable () {
                    public void run() {
                        try {
                            server[0] = (SSLSocket) c.serverSocket.accept();
                            server[0].startHandshake();
                        } catch (RuntimeException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            thread.start();
            SSLSocket client = (SSLSocket)
                c.sslContext.getSocketFactory().createSocket(c.host, c.port);
            if (clientCipherSuites != null) {
                client.setEnabledCipherSuites(clientCipherSuites);
            }
            client.startHandshake();
            thread.join();
            return new SSLSocket[] { server[0], client };
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

