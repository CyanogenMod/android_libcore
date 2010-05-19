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

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.Key;
import java.security.KeyStore;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;

public class SSLSocketTest extends TestCase {

    public void test_SSLSocket_getSupportedCipherSuites_names() throws Exception {
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket ssl = (SSLSocket) sf.createSocket();
        String[] cipherSuites = ssl.getSupportedCipherSuites();
        assertNotNull(cipherSuites);
        assertTrue(cipherSuites.length != 0);
        Set remainingCipherSuites = new HashSet<String>(StandardNames.CIPHER_SUITES);
        for (String cipherSuite : cipherSuites) {
            assertTrue(remainingCipherSuites.remove(cipherSuite));
        }
        assertEquals(Collections.EMPTY_SET, remainingCipherSuites);
        assertEquals(StandardNames.CIPHER_SUITES.size(), cipherSuites.length);
    }

    public void test_SSLSocket_getSupportedCipherSuites_connect() throws Exception {
        TestSSLContext c = TestSSLContext.create();
        String[] cipherSuites = c.sslContext.getSocketFactory().getSupportedCipherSuites();
        for (String cipherSuite : cipherSuites) {
            if (cipherSuite.startsWith("TLS_KRB5_")) {
                /*
                 * Kerberos cipher suites require external setup. See "Kerberos Requirements" in
                 * https://java.sun.com/j2se/1.5.0/docs/guide/security/jsse/JSSERefGuide.html#KRBRequire
                 */
                continue;
            }
            // System.out.println("Trying to connect cipher suite " + cipherSuite);
            String[] cipherSuiteArray = new String[] { cipherSuite };
            TestSSLSocketPair.connect(c, cipherSuiteArray, cipherSuiteArray);
        }
    }

    public void test_SSLSocket_getEnabledCipherSuites() throws Exception {
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket ssl = (SSLSocket) sf.createSocket();
        String[] cipherSuites = ssl.getEnabledCipherSuites();
        assertNotNull(cipherSuites);
        assertTrue(cipherSuites.length != 0);

        // Make sure modifying the result is not observable
        String savedCipherSuite = cipherSuites[0];
        assertNotNull(savedCipherSuite);
        cipherSuites[0] = null;
        assertNotNull(ssl.getEnabledCipherSuites()[0]);
        cipherSuites[0] = savedCipherSuite;

        // Make sure all cipherSuites names are expected
        for (String cipherSuite : cipherSuites) {
            assertTrue(StandardNames.CIPHER_SUITES.contains(cipherSuite));
        }
    }

    public void test_SSLSocket_setEnabledCipherSuites() throws Exception {
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket ssl = (SSLSocket) sf.createSocket();

        try {
            ssl.setEnabledCipherSuites(null);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            ssl.setEnabledCipherSuites(new String[1]);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            ssl.setEnabledCipherSuites(new String[] { "Bogus" } );
            fail();
        } catch (IllegalArgumentException expected) {
        }

        ssl.setEnabledCipherSuites(new String[0]);
        ssl.setEnabledCipherSuites(ssl.getEnabledCipherSuites());
        ssl.setEnabledCipherSuites(ssl.getSupportedCipherSuites());
    }

    public void test_SSLSocket_getSupportedProtocols() throws Exception {
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket ssl = (SSLSocket) sf.createSocket();
        String[] protocols = ssl.getSupportedProtocols();
        assertNotNull(protocols);
        assertTrue(protocols.length != 0);

        // Make sure modifying the result is not observable
        String savedProtocol = protocols[0];
        assertNotNull(savedProtocol);
        protocols[0] = null;
        assertNotNull(ssl.getSupportedProtocols()[0]);
        protocols[0] = savedProtocol;

        // Make sure all protocol names are expected
        for (String protocol : protocols) {
            assertNotNull(StandardNames.SSL_SOCKET_PROTOCOLS.contains(protocol));
        }
    }

    public void test_SSLSocket_getEnabledProtocols() throws Exception {
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket ssl = (SSLSocket) sf.createSocket();
        String[] protocols = ssl.getEnabledProtocols();
        assertNotNull(protocols);
        assertTrue(protocols.length != 0);

        // Make sure modifying the result is not observable
        String savedProtocol = protocols[0];
        assertNotNull(savedProtocol);
        protocols[0] = null;
        assertNotNull(ssl.getEnabledProtocols()[0]);
        protocols[0] = savedProtocol;

        // Make sure all protocol names are expected
        for (String protocol : protocols) {
            assertNotNull(StandardNames.SSL_SOCKET_PROTOCOLS.contains(protocol));
        }
    }

    public void test_SSLSocket_setEnabledProtocols() throws Exception {
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket ssl = (SSLSocket) sf.createSocket();

        try {
            ssl.setEnabledProtocols(null);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            ssl.setEnabledProtocols(new String[1]);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            ssl.setEnabledProtocols(new String[] { "Bogus" } );
            fail();
        } catch (IllegalArgumentException expected) {
        }
        ssl.setEnabledProtocols(new String[0]);
        ssl.setEnabledProtocols(ssl.getEnabledProtocols());
        ssl.setEnabledProtocols(ssl.getSupportedProtocols());
    }

    public void test_SSLSocket_getSession() throws Exception {
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket ssl = (SSLSocket) sf.createSocket();
        SSLSession session = ssl.getSession();
        assertNotNull(session);
        assertFalse(session.isValid());
    }

    public void test_SSLSocket_startHandshake() throws Exception {
        final TestSSLContext c = TestSSLContext.create();
        SSLSocket client = (SSLSocket) c.sslContext.getSocketFactory().createSocket(c.host, c.port);
        final SSLSocket server = (SSLSocket) c.serverSocket.accept();
        Thread thread = new Thread(new Runnable () {
            public void run() {
                try {
                    server.startHandshake();
                    assertNotNull(server.getSession());
                    try {
                        server.getSession().getPeerCertificates();
                        fail();
                    } catch (SSLPeerUnverifiedException expected) {
                    }
                    Certificate[] localCertificates = server.getSession().getLocalCertificates();
                    assertNotNull(localCertificates);
                    assertEquals(1, localCertificates.length);
                    assertNotNull(localCertificates[0]);
                    TestSSLContext.assertCertificateInKeyStore(localCertificates[0], c.keyStore);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
        client.startHandshake();
        assertNotNull(client.getSession());
        assertNull(client.getSession().getLocalCertificates());
        Certificate[] peerCertificates = client.getSession().getPeerCertificates();
        assertNotNull(peerCertificates);
        assertEquals(1, peerCertificates.length);
        assertNotNull(peerCertificates[0]);
        TestSSLContext.assertCertificateInKeyStore(peerCertificates[0], c.keyStore);
        thread.join();
    }

    public void test_SSLSocket_startHandshake_noKeyStore() throws Exception {
        TestSSLContext c = TestSSLContext.create(null, null);
        SSLSocket client = (SSLSocket) c.sslContext.getSocketFactory().createSocket(c.host, c.port);
        try {
            SSLSocket server = (SSLSocket) c.serverSocket.accept();
            fail();
        } catch (SSLException expected) {
        }
    }

    public void test_SSLSocket_startHandshake_noClientCertificate() throws Exception {
        TestSSLContext serverContext = TestSSLContext.create();
        TestSSLContext clientContext = TestSSLContext.createClient(serverContext);
        SSLSocket client = (SSLSocket)
            clientContext.sslContext.getSocketFactory().createSocket(serverContext.host,
                                                                     serverContext.port);
        final SSLSocket server = (SSLSocket) serverContext.serverSocket.accept();
        Thread thread = new Thread(new Runnable () {
            public void run() {
                try {
                    server.startHandshake();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
        client.startHandshake();
        thread.join();
    }

    public void test_SSLSocket_HandshakeCompletedListener() throws Exception {
        final TestSSLContext c = TestSSLContext.create();
        final SSLSocket client = (SSLSocket) c.sslContext.getSocketFactory().createSocket(c.host,
                                                                                          c.port);
        final SSLSocket server = (SSLSocket) c.serverSocket.accept();
        Thread thread = new Thread(new Runnable () {
            public void run() {
                try {
                    server.startHandshake();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
        final boolean[] handshakeCompletedListenerCalled = new boolean[1];
        client.addHandshakeCompletedListener(new HandshakeCompletedListener() {
            public void handshakeCompleted(HandshakeCompletedEvent event) {
                try {
                    SSLSession session = event.getSession();
                    String cipherSuite = event.getCipherSuite();
                    Certificate[] localCertificates = event.getLocalCertificates();
                    Certificate[] peerCertificates = event.getPeerCertificates();
                    javax.security.cert.X509Certificate[] peerCertificateChain
                            = event.getPeerCertificateChain();
                    Principal peerPrincipal = event.getPeerPrincipal();
                    Principal localPrincipal = event.getLocalPrincipal();
                    Socket socket = event.getSocket();

                    if (false) {
                        System.out.println("Session=" + session);
                        System.out.println("CipherSuite=" + cipherSuite);
                        System.out.println("LocalCertificates=" + localCertificates);
                        System.out.println("PeerCertificates=" + peerCertificates);
                        System.out.println("PeerCertificateChain=" + peerCertificateChain);
                        System.out.println("PeerPrincipal=" + peerPrincipal);
                        System.out.println("LocalPrincipal=" + localPrincipal);
                        System.out.println("Socket=" + socket);
                    }

                    assertNotNull(session);
                    byte[] id = session.getId();
                    assertNotNull(id);
                    assertEquals(32, id.length);
                    assertNotNull(c.sslContext.getClientSessionContext().getSession(id));
                    if (!TestSSLContext.sslServerSocketSupportsSessionTickets()) {
                        assertNotNull(c.sslContext.getServerSessionContext().getSession(id));
                    }

                    assertNotNull(cipherSuite);
                    assertTrue(Arrays.asList(
                            client.getEnabledCipherSuites()).contains(cipherSuite));
                    assertTrue(Arrays.asList(
                            c.serverSocket.getEnabledCipherSuites()).contains(cipherSuite));

                    assertNull(localCertificates);

                    assertNotNull(peerCertificates);
                    assertEquals(1, peerCertificates.length);
                    assertNotNull(peerCertificates[0]);
                    TestSSLContext.assertCertificateInKeyStore(peerCertificates[0], c.keyStore);

                    assertNotNull(peerCertificateChain);
                    assertEquals(1, peerCertificateChain.length);
                    assertNotNull(peerCertificateChain[0]);
                    TestSSLContext.assertCertificateInKeyStore(
                        peerCertificateChain[0].getSubjectDN(), c.keyStore);

                    assertNotNull(peerPrincipal);
                    TestSSLContext.assertCertificateInKeyStore(peerPrincipal, c.keyStore);

                    assertNull(localPrincipal);

                    assertNotNull(socket);
                    assertSame(client, socket);

                    synchronized (handshakeCompletedListenerCalled) {
                        handshakeCompletedListenerCalled[0] = true;
                        handshakeCompletedListenerCalled.notify();
                    }
                    handshakeCompletedListenerCalled[0] = true;
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        client.startHandshake();
        thread.join();
        synchronized (handshakeCompletedListenerCalled) {
            while (!handshakeCompletedListenerCalled[0]) {
                handshakeCompletedListenerCalled.wait();
            }
        }
    }

    public void test_SSLSocket_HandshakeCompletedListener_RuntimeException() throws Exception {
        final TestSSLContext c = TestSSLContext.create();
        final SSLSocket client = (SSLSocket) c.sslContext.getSocketFactory().createSocket(c.host,
                                                                                          c.port);
        final SSLSocket server = (SSLSocket) c.serverSocket.accept();
        Thread thread = new Thread(new Runnable () {
            public void run() {
                try {
                    server.startHandshake();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
        client.addHandshakeCompletedListener(new HandshakeCompletedListener() {
            public void handshakeCompleted(HandshakeCompletedEvent event) {
                throw new RuntimeException("RuntimeException from handshakeCompleted");
            }
        });
        client.startHandshake();
        thread.join();
    }

    public void test_SSLSocket_getUseClientMode() throws Exception {
        TestSSLContext c = TestSSLContext.create();
        SSLSocket client = (SSLSocket) c.sslContext.getSocketFactory().createSocket(c.host, c.port);
        SSLSocket server = (SSLSocket) c.serverSocket.accept();
        assertTrue(client.getUseClientMode());
        assertFalse(server.getUseClientMode());
    }

    public void test_SSLSocket_setUseClientMode() throws Exception {
        // client is client, server is server
        test_SSLSocket_setUseClientMode(true, false);
        // client is server, server is client
        test_SSLSocket_setUseClientMode(true, false);
        // both are client
        try {
            test_SSLSocket_setUseClientMode(true, true);
            fail();
        } catch (SSLProtocolException expected) {
        }

        // both are server
        try {
            test_SSLSocket_setUseClientMode(false, false);
            fail();
        } catch (SocketTimeoutException expected) {
        }
    }

    private void test_SSLSocket_setUseClientMode(final boolean clientClientMode,
                                                 final boolean serverClientMode)
            throws Exception {
        TestSSLContext c = TestSSLContext.create();
        SSLSocket client = (SSLSocket) c.sslContext.getSocketFactory().createSocket(c.host, c.port);
        final SSLSocket server = (SSLSocket) c.serverSocket.accept();

        final SSLProtocolException[] sslProtocolException = new SSLProtocolException[1];
        final SocketTimeoutException[] socketTimeoutException = new SocketTimeoutException[1];
        Thread thread = new Thread(new Runnable () {
            public void run() {
                try {
                    if (!serverClientMode) {
                        server.setSoTimeout(1 * 1000);
                    }
                    server.setUseClientMode(serverClientMode);
                    server.startHandshake();
                } catch (SSLProtocolException e) {
                    sslProtocolException[0] = e;
                } catch (SocketTimeoutException e) {
                    socketTimeoutException[0] = e;
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
        if (!clientClientMode) {
            client.setSoTimeout(1 * 1000);
        }
        client.setUseClientMode(clientClientMode);
        client.startHandshake();
        thread.join();
        if (sslProtocolException[0] != null) {
            throw sslProtocolException[0];
        }
        if (socketTimeoutException[0] != null) {
            throw socketTimeoutException[0];
        }
    }

    public void test_SSLSocket_clientAuth() throws Exception {
        TestSSLContext c = TestSSLContext.create();
        SSLSocket client = (SSLSocket) c.sslContext.getSocketFactory().createSocket(c.host, c.port);
        final SSLSocket server = (SSLSocket) c.serverSocket.accept();
        Thread thread = new Thread(new Runnable () {
            public void run() {
                try {
                    assertFalse(server.getWantClientAuth());
                    assertFalse(server.getNeedClientAuth());

                    // confirm turning one on by itself
                    server.setWantClientAuth(true);
                    assertTrue(server.getWantClientAuth());
                    assertFalse(server.getNeedClientAuth());

                    // confirm turning setting on toggles the other
                    server.setNeedClientAuth(true);
                    assertFalse(server.getWantClientAuth());
                    assertTrue(server.getNeedClientAuth());

                    // confirm toggling back
                    server.setWantClientAuth(true);
                    assertTrue(server.getWantClientAuth());
                    assertFalse(server.getNeedClientAuth());

                    server.startHandshake();

                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
        client.startHandshake();
        assertNotNull(client.getSession().getLocalCertificates());
        assertEquals(1, client.getSession().getLocalCertificates().length);
        thread.join();
    }

    public void test_SSLSocket_getEnableSessionCreation() throws Exception {
        TestSSLContext c = TestSSLContext.create();
        SSLSocket client = (SSLSocket) c.sslContext.getSocketFactory().createSocket(c.host, c.port);
        SSLSocket server = (SSLSocket) c.serverSocket.accept();
        assertTrue(client.getEnableSessionCreation());
        assertTrue(server.getEnableSessionCreation());
    }

    public void test_SSLSocket_setEnableSessionCreation_server() throws Exception {
        TestSSLContext c = TestSSLContext.create();
        SSLSocket client = (SSLSocket) c.sslContext.getSocketFactory().createSocket(c.host, c.port);
        final SSLSocket server = (SSLSocket) c.serverSocket.accept();
        Thread thread = new Thread(new Runnable () {
            public void run() {
                try {
                    server.setEnableSessionCreation(false);
                    try {
                        server.startHandshake();
                        fail();
                    } catch (SSLException expected) {
                    }
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
        try {
            client.startHandshake();
            fail();
        } catch (SSLException expected) {
        }
        thread.join();
    }

    public void test_SSLSocket_setEnableSessionCreation_client() throws Exception {
        TestSSLContext c = TestSSLContext.create();
        SSLSocket client = (SSLSocket) c.sslContext.getSocketFactory().createSocket(c.host, c.port);
        final SSLSocket server = (SSLSocket) c.serverSocket.accept();
        Thread thread = new Thread(new Runnable () {
            public void run() {
                try {
                    try {
                        server.startHandshake();
                        fail();
                    } catch (SSLException expected) {
                    }
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
        client.setEnableSessionCreation(false);
        try {
            client.startHandshake();
            fail();
        } catch (SSLException expected) {
        }
        thread.join();
    }

    public void test_SSLSocketTest_Test_create() {
        TestSSLSocketPair test = TestSSLSocketPair.create();
        assertNotNull(test.c);
        assertNotNull(test.server);
        assertNotNull(test.client);
        assertTrue(test.server.isConnected());
        assertTrue(test.client.isConnected());
        assertNotNull(test.server.getSession());
        assertNotNull(test.client.getSession());
    }

    /**
     * Not run by default by JUnit, but can be run by Vogar by
     * specifying it explictly (or with main method below)
     */
    public void stress_test_SSLSocketTest_Test_create() {
        final boolean verbose = true;
        while (true) {
            TestSSLSocketPair test = TestSSLSocketPair.create();
            if (verbose) {
                System.out.println("client=" + test.client.getLocalPort()
                                   + " server=" + test.server.getLocalPort());
            } else {
                System.out.print("X");
            }
        }
    }

    public static final void main (String[] args) {
        new SSLSocketTest().stress_test_SSLSocketTest_Test_create();
    }
}
