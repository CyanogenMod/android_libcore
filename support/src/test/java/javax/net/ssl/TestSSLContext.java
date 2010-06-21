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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.Security;
import java.security.StandardNames;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import junit.framework.Assert;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * TestSSLContext is a convenience class for other tests that
 * want a canned SSLContext and related state for testing so they
 * don't have to duplicate the logic.
 */
public final class TestSSLContext extends Assert {

    /*
     * The RI and Android have very different default SSLSession cache behaviors.
     * The RI keeps an unlimited number of SSLSesions around for 1 day.
     * Android keeps 10 SSLSessions forever.
     */
    private static final boolean IS_RI = StandardNames.IS_RI;
    public static final int EXPECTED_DEFAULT_CLIENT_SSL_SESSION_CACHE_SIZE = (IS_RI) ? 0 : 10;
    public static final int EXPECTED_DEFAULT_SERVER_SSL_SESSION_CACHE_SIZE = (IS_RI) ? 0 : 100;
    public static final int EXPECTED_DEFAULT_SSL_SESSION_CACHE_TIMEOUT = (IS_RI) ? 86400 : 0;
    static {
        if (IS_RI) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * The Android SSLSocket and SSLServerSocket implementations are
     * based on a version of OpenSSL which includes support for RFC
     * 4507 session tickets. When using session tickets, the server
     * does not need to keep a cache mapping session IDs to SSL
     * sessions for reuse. Instead, the client presents the server
     * with a session ticket it received from the server earlier,
     * which is an SSL session encrypted by the server's secret
     * key. Since in this case the server does not need to keep a
     * cache, some tests may find different results depending on
     * whether or not the session tickets are in use. These tests can
     * use this function to determine if loopback SSL connections are
     * expected to use session tickets and conditionalize their
     * results appropriately.
     */
    public static boolean sslServerSocketSupportsSessionTickets () {
        // Disabled session tickets for better compatability b/2682876
        // return !IS_RI;
        return false;
    }

    public final KeyStore keyStore;
    public final char[] keyStorePassword;
    public final SSLContext sslContext;
    public final SSLServerSocket serverSocket;
    public final InetAddress host;
    public final int port;

    private TestSSLContext(KeyStore keyStore,
                           char[] keyStorePassword,
                           SSLContext sslContext,
                           SSLServerSocket serverSocket,
                           InetAddress host,
                           int port) {
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.sslContext = sslContext;
        this.serverSocket = serverSocket;
        this.host = host;
        this.port = port;
    }

    /**
     * Usual TestSSLContext creation method, creates underlying
     * SSLContext with certificate and key as well as SSLServerSocket
     * listening provided host and port.
     */
    public static TestSSLContext create() {
        TestKeyStore testKeyStore = TestKeyStore.get();
        return create(testKeyStore.keyStore, testKeyStore.keyStorePassword);
    }

    /**
     * TestSSLContext creation method that allows separate creation of key store
     */
    public static TestSSLContext create(KeyStore keyStore, char[] keyStorePassword) {
        try {
            SSLContext sslContext = createSSLContext(keyStore, keyStorePassword);

            SSLServerSocket serverSocket = (SSLServerSocket)
                sslContext.getServerSocketFactory().createServerSocket(0);
            InetSocketAddress sa = (InetSocketAddress) serverSocket.getLocalSocketAddress();
            InetAddress host = sa.getAddress();
            int port = sa.getPort();

            return new TestSSLContext(keyStore, keyStorePassword,
                                      sslContext, serverSocket, host, port);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a client version of the server TestSSLContext. The
     * client will trust the server's certificate, but not contain any
     * keys of its own.
     */
    public static TestSSLContext createClient(TestSSLContext server) {
        try {
            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(null, null);
            for (String alias: Collections.list(server.keyStore.aliases())) {
                if (!server.keyStore.isCertificateEntry(alias)) {
                    continue;
                }
                Certificate cert = server.keyStore.getCertificate(alias);
                keyStore.setCertificateEntry(alias, cert);
            }

            char[] keyStorePassword = server.keyStorePassword;

            String tmfa = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfa);
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

            return new TestSSLContext(keyStore, keyStorePassword,
                                      sslContext, null, null, -1);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a SSLContext with a KeyManager using the private key and
     * certificate chain from the given KeyStore and a TrustManager
     * using the certificates authorities from the same KeyStore.
     */
    public static final SSLContext createSSLContext(final KeyStore keyStore,
                                                    final char[] keyStorePassword)
        throws Exception {
        String kmfa = KeyManagerFactory.getDefaultAlgorithm();
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(kmfa);
        kmf.init(keyStore, keyStorePassword);

        String tmfa = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfa);
        tmf.init(keyStore);

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return context;
    }

    public static void assertCertificateInKeyStore(Principal principal,
                                                   KeyStore keyStore) throws Exception {
        String subjectName = principal.getName();
        boolean found = false;
        for (String alias: Collections.list(keyStore.aliases())) {
            if (!keyStore.isCertificateEntry(alias)) {
                continue;
            }
            X509Certificate keyStoreCertificate = (X509Certificate) keyStore.getCertificate(alias);
            if (subjectName.equals(keyStoreCertificate.getSubjectDN().getName())) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    public static void assertCertificateInKeyStore(Certificate certificate,
                                                   KeyStore keyStore) throws Exception {
        boolean found = false;
        for (String alias: Collections.list(keyStore.aliases())) {
            if (!keyStore.isCertificateEntry(alias)) {
                continue;
            }
            Certificate keyStoreCertificate = keyStore.getCertificate(alias);
            if (certificate.equals(keyStoreCertificate)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }
}
