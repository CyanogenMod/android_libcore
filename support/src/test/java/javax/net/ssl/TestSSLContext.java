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

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Hashtable;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

/**
 * TestSSLContext is a convenience class for other tests that
 * want a canned SSLContext and related state for testing so they
 * don't have to duplicate the logic.
 */
public final class TestSSLContext {

    public static final boolean IS_RI = !"Dalvik Core Library".equals(System.getProperty("java.specification.name"));
    public static final String PROVIDER_NAME = (IS_RI) ? "SunJSSE" : "HarmonyJSSE";

    /*
     * The RI and Android have very different default SSLSession cache behaviors.
     * The RI keeps an unlimited number of SSLSesions around for 1 day.
     * Android keeps 10 SSLSessions forever.
     */
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
        return !IS_RI;
    }

    public final KeyStore keyStore;
    public final char[] keyStorePassword;
    public final String publicAlias;
    public final String privateAlias;
    public final SSLContext sslContext;
    public final SSLServerSocket serverSocket;
    public final InetAddress host;
    public final int port;

    private TestSSLContext(KeyStore keyStore,
                           char[] keyStorePassword,
                           String publicAlias,
                           String privateAlias,
                           SSLContext sslContext,
                           SSLServerSocket serverSocket,
                           InetAddress host,
                           int port) {
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.publicAlias = publicAlias;
        this.privateAlias = privateAlias;
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
        try {
            char[] keyStorePassword = null;
            String publicAlias = "public";
            String privateAlias = "private";
            return create(createKeyStore(keyStorePassword, publicAlias, privateAlias),
                          keyStorePassword,
                          publicAlias,
                          privateAlias);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * TestSSLContext creation method that allows separate creation of key store
     */
    public static TestSSLContext create(KeyStore keyStore,
                                        char[] keyStorePassword,
                                        String publicAlias,
                                        String privateAlias) {
        try {
            SSLContext sslContext = createSSLContext(keyStore, keyStorePassword);

            SSLServerSocket serverSocket = (SSLServerSocket)
                sslContext.getServerSocketFactory().createServerSocket(0);
            InetSocketAddress sa = (InetSocketAddress) serverSocket.getLocalSocketAddress();
            InetAddress host = sa.getAddress();
            int port = sa.getPort();

            return new TestSSLContext(keyStore, keyStorePassword, publicAlias, privateAlias,
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
            String publicAlias = server.publicAlias;
            Certificate cert = server.keyStore.getCertificate(publicAlias);

            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(null, null);
            keyStore.setCertificateEntry(publicAlias, cert);

            char[] keyStorePassword = server.keyStorePassword;
            String privateAlias = null;

            String tmfa = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfa);
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

            return new TestSSLContext(keyStore, keyStorePassword, publicAlias, publicAlias,
                                      sslContext, null, null, -1);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a BKS KeyStore containing an RSAPrivateKey with alias
     * "private" and a X509Certificate based on the matching
     * RSAPublicKey stored under the alias name publicAlias.
     *
     * The private key will have a certificate chain including the
     * certificate stored under the alias name privateAlias. The
     * certificate will be signed by the private key. The certificate
     * Subject and Issuer Common-Name will be the local host's
     * canonical hostname. The certificate will be valid for one day
     * before and one day after the time of creation.
     *
     * The KeyStore is optionally password protected by the
     * keyStorePassword argument, which can be null if a password is
     * not desired.
     *
     * Based on:
     * org.bouncycastle.jce.provider.test.SigTest
     * org.bouncycastle.jce.provider.test.CertTest
     */
    public static KeyStore createKeyStore(char[] keyStorePassword,
                                          String publicAlias,
                                          String privateAlias)
        throws Exception {

        RSAPrivateKey privateKey;
        X509Certificate x509c;
        if (publicAlias == null && privateAlias == null) {
            // don't want anything apparently
            privateKey = null;
            x509c = null;
        } else {
            // 1.) we make the keys
            int keysize = 1024;
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(keysize, new SecureRandom());
            KeyPair kp = kpg.generateKeyPair();
            privateKey = (RSAPrivateKey)kp.getPrivate();
            RSAPublicKey publicKey  = (RSAPublicKey)kp.getPublic();

            // 2.) use keys to make certficate

            // note that there doesn't seem to be a standard way to make a
            // certificate using java.* or javax.*. The CertificateFactory
            // interface assumes you want to read in a stream of bytes a
            // factory specific format. So here we use Bouncy Castle's
            // X509V3CertificateGenerator and related classes.

            Hashtable attributes = new Hashtable();
            attributes.put(X509Principal.CN, InetAddress.getLocalHost().getCanonicalHostName());
            X509Principal dn = new X509Principal(attributes);

            long millisPerDay = 24 * 60 * 60 * 1000;
            long now = System.currentTimeMillis();
            Date start = new Date(now - millisPerDay);
            Date end = new Date(now + millisPerDay);
            BigInteger serial = BigInteger.valueOf(1);

            X509V3CertificateGenerator x509cg = new X509V3CertificateGenerator();
            x509cg.setSubjectDN(dn);
            x509cg.setIssuerDN(dn);
            x509cg.setNotBefore(start);
            x509cg.setNotAfter(end);
            x509cg.setPublicKey(publicKey);
            x509cg.setSignatureAlgorithm("sha1WithRSAEncryption");
            x509cg.setSerialNumber(serial);
            x509c = x509cg.generateX509Certificate(privateKey);
        }

        X509Certificate[] x509cc;
        if (privateAlias == null) {
            // don't need certificate chain
            x509cc = null;
        } else {
            x509cc = new X509Certificate[] { x509c };
        }

        // 3.) put certificate and private key to make a key store
        KeyStore ks = KeyStore.getInstance("BKS");
        ks.load(null, null);
        if (privateAlias != null) {
            ks.setKeyEntry(privateAlias, privateKey, keyStorePassword, x509cc);
        }
        if (publicAlias != null) {
            ks.setCertificateEntry(publicAlias, x509c);
        }
        return ks;
    }

    /**
     * Create a SSLContext with a KeyManager using the private key and
     * certificate chain from the given KeyStore and a TrustManager
     * using the certificates authorities from the same KeyStore.
     */
    public static final SSLContext createSSLContext(final KeyStore keyStore, final char[] keyStorePassword)
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
}
