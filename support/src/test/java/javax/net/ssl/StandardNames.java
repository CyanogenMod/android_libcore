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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import junit.framework.Assert;

/**
 * This class defines expected string names for protocols, key types,
 * client and server auth types, cipher suites.
 *
 * Initially based on "Appendix A: Standard Names" of
 * <a href="http://java.sun.com/j2se/1.5.0/docs/guide/security/jsse/JSSERefGuide.html#AppA">
 * Java Secure Socket Extension (JSSE) Reference Guide for the JavaTM 2 Platform Standard Edition 5
 * </a>
 *
 * Updated based on the "The SunJSSE Provider" section of
 * <a href="java.sun.com/javase/6/docs/technotes/guides/security/SunProviders.html#SunJSSEProvider">
 * Java Cryptography Architecture Sun Providers Documentation for JavaTM Platform Standard Edition 6
 * </a>
 */
public final class StandardNames extends Assert {

    public static final String SSL_CONTEXT_PROTOCOLS_DEFAULT = "Default";
    public static final Set<String> SSL_CONTEXT_PROTOCOLS = new HashSet<String>(Arrays.asList(
        SSL_CONTEXT_PROTOCOLS_DEFAULT,
        "SSL",
        // "SSLv2",
        "SSLv3",
        "TLS",
        "TLSv1"));
    public static final String SSL_CONTEXT_PROTOCOL_DEFAULT = "TLS";

    public static final Set<String> KEY_TYPES = new HashSet<String>(Arrays.asList(
        "RSA",
        "DSA",
        "DH_RSA",
        "DH_DSA"));

    public static final Set<String> SSL_SOCKET_PROTOCOLS = new HashSet<String>(Arrays.asList(
        // "SSLv2",
        "SSLv3",
        "TLSv1"));
    static {
        if (TestSSLContext.IS_RI) {
            /* Even though we use OpenSSL's SSLv23_method which
             * supports sending SSLv2 client hello messages, the
             * OpenSSL implementation in s23_client_hello disables
             * this if SSL_OP_NO_SSLv2 is specified, which we always
             * do to disable general use of SSLv2.
             */
            SSL_SOCKET_PROTOCOLS.add("SSLv2Hello");
        }
    }

    public static final Set<String> CLIENT_AUTH_TYPES = new HashSet<String>(KEY_TYPES);

    public static final Set<String> SERVER_AUTH_TYPES = new HashSet<String>(Arrays.asList(
        "DHE_DSS",
        "DHE_DSS_EXPORT",
        "DHE_RSA",
        "DHE_RSA_EXPORT",
        "DH_DSS_EXPORT",
        "DH_RSA_EXPORT",
        "DH_anon",
        "DH_anon_EXPORT",
        "KRB5",
        "KRB5_EXPORT",
        "RSA",
        "RSA_EXPORT",
        "RSA_EXPORT1024",
        "UNKNOWN"));

    public static final String CIPHER_SUITE_INVALID = "SSL_NULL_WITH_NULL_NULL";

    public static final Set<String> CIPHER_SUITES_NEITHER = new HashSet<String>();

    public static final Set<String> CIPHER_SUITES_RI = new LinkedHashSet<String>();
    public static final Set<String> CIPHER_SUITES_OPENSSL = new LinkedHashSet<String>();

    public static final Set<String> CIPHER_SUITES;

    private static final void addRi(String cipherSuite) {
        CIPHER_SUITES_RI.add(cipherSuite);
    }

    private static final void addOpenSsl(String cipherSuite) {
        CIPHER_SUITES_OPENSSL.add(cipherSuite);
    }

    private static final void addBoth(String cipherSuite) {
        addRi(cipherSuite);
        addOpenSsl(cipherSuite);
    }

    private static final void addNeither(String cipherSuite) {
        CIPHER_SUITES_NEITHER.add(cipherSuite);
    }

    static {
        // Note these are added in priority order as defined by RI 6 documentation.
        // Android currently does not support Elliptic Curve or Diffie-Hellman
        addBoth(   "SSL_RSA_WITH_RC4_128_MD5");
        addBoth(   "SSL_RSA_WITH_RC4_128_SHA");
        addBoth(   "TLS_RSA_WITH_AES_128_CBC_SHA");
        addOpenSsl("TLS_RSA_WITH_AES_256_CBC_SHA");
        addNeither("TLS_ECDH_ECDSA_WITH_RC4_128_SHA");
        addNeither("TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA");
        addNeither("TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA");
        addNeither("TLS_ECDH_RSA_WITH_RC4_128_SHA");
        addNeither("TLS_ECDH_RSA_WITH_AES_128_CBC_SHA");
        addNeither("TLS_ECDH_RSA_WITH_AES_256_CBC_SHA");
        addNeither("TLS_ECDHE_ECDSA_WITH_RC4_128_SHA");
        addNeither("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA");
        addNeither("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA");
        addNeither("TLS_ECDHE_RSA_WITH_RC4_128_SHA");
        addNeither("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
        addNeither("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA");
        addBoth(   "TLS_DHE_RSA_WITH_AES_128_CBC_SHA");
        addOpenSsl("TLS_DHE_RSA_WITH_AES_256_CBC_SHA");
        addBoth(   "TLS_DHE_DSS_WITH_AES_128_CBC_SHA");
        addOpenSsl("TLS_DHE_DSS_WITH_AES_256_CBC_SHA");
        addBoth(   "SSL_RSA_WITH_3DES_EDE_CBC_SHA");
        addNeither("TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA");
        addNeither("TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA");
        addNeither("TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA");
        addNeither("TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA");
        addBoth(   "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA");
        addBoth(   "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA");
        addBoth(   "SSL_RSA_WITH_DES_CBC_SHA");
        addBoth(   "SSL_DHE_RSA_WITH_DES_CBC_SHA");
        addBoth(   "SSL_DHE_DSS_WITH_DES_CBC_SHA");
        addBoth(   "SSL_RSA_EXPORT_WITH_RC4_40_MD5");
        addBoth(   "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA");
        addBoth(   "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA");
        addBoth(   "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
        addBoth(   "SSL_RSA_WITH_NULL_MD5");
        addBoth(   "SSL_RSA_WITH_NULL_SHA");
        addNeither("TLS_ECDH_ECDSA_WITH_NULL_SHA");
        addNeither("TLS_ECDH_RSA_WITH_NULL_SHA");
        addNeither("TLS_ECDHE_ECDSA_WITH_NULL_SHA");
        addNeither("TLS_ECDHE_RSA_WITH_NULL_SHA");
        addBoth(   "SSL_DH_anon_WITH_RC4_128_MD5");
        addBoth(   "TLS_DH_anon_WITH_AES_128_CBC_SHA");
        addOpenSsl("TLS_DH_anon_WITH_AES_256_CBC_SHA");
        addBoth(   "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA");
        addBoth(   "SSL_DH_anon_WITH_DES_CBC_SHA");
        addNeither("TLS_ECDH_anon_WITH_RC4_128_SHA");
        addNeither("TLS_ECDH_anon_WITH_AES_128_CBC_SHA");
        addNeither("TLS_ECDH_anon_WITH_AES_256_CBC_SHA");
        addNeither("TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA");
        addBoth(   "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5");
        addBoth(   "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA");
        addNeither("TLS_ECDH_anon_WITH_NULL_SHA");

        // Android does not have Keberos support
        addRi     ("TLS_KRB5_WITH_RC4_128_SHA");
        addRi     ("TLS_KRB5_WITH_RC4_128_MD5");
        addRi     ("TLS_KRB5_WITH_3DES_EDE_CBC_SHA");
        addRi     ("TLS_KRB5_WITH_3DES_EDE_CBC_MD5");
        addRi     ("TLS_KRB5_WITH_DES_CBC_SHA");
        addRi     ("TLS_KRB5_WITH_DES_CBC_MD5");
        addRi     ("TLS_KRB5_EXPORT_WITH_RC4_40_SHA");
        addRi     ("TLS_KRB5_EXPORT_WITH_RC4_40_MD5");
        addRi     ("TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA");
        addRi     ("TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5");

        // Dropped
        addNeither("SSL_DH_DSS_EXPORT_WITH_DES40_CBC_SHA");
        addNeither("SSL_DH_RSA_EXPORT_WITH_DES40_CBC_SHA");

        // Old non standard exportable encryption
        addNeither("SSL_RSA_EXPORT1024_WITH_DES_CBC_SHA");
        addNeither("SSL_RSA_EXPORT1024_WITH_RC4_56_SHA");

        // No RC2
        addNeither("SSL_RSA_EXPORT_WITH_RC2_CBC_40_MD5");
        addNeither("TLS_KRB5_EXPORT_WITH_RC2_CBC_40_SHA");
        addNeither("TLS_KRB5_EXPORT_WITH_RC2_CBC_40_MD5");

        CIPHER_SUITES = (TestSSLContext.IS_RI) ? CIPHER_SUITES_RI : CIPHER_SUITES_OPENSSL;
    }

    public static final Set<String> CIPHER_SUITES_SSLENGINE = new HashSet<String>(CIPHER_SUITES);
    static {
        if (!TestSSLContext.IS_RI) {
            // Android does not include Java versions of RC4 and IDEA
            // Java crypto implementations so these fail to work for
            // the SSLEngine implementation.
            CIPHER_SUITES_SSLENGINE.remove("SSL_DH_anon_EXPORT_WITH_RC4_40_MD5");
            CIPHER_SUITES_SSLENGINE.remove("SSL_DH_anon_WITH_RC4_128_MD5");
            CIPHER_SUITES_SSLENGINE.remove("SSL_RSA_EXPORT_WITH_RC2_CBC_40_MD5");
            CIPHER_SUITES_SSLENGINE.remove("SSL_RSA_EXPORT_WITH_RC4_40_MD5");
            CIPHER_SUITES_SSLENGINE.remove("SSL_RSA_WITH_RC4_128_MD5");
            CIPHER_SUITES_SSLENGINE.remove("SSL_RSA_WITH_RC4_128_SHA");
            CIPHER_SUITES_SSLENGINE.remove("TLS_RSA_WITH_IDEA_CBC_SHA");
            // Harmony SSLEngine does not support AES cipher suites
            // that are supported by the OpenSSL based SSLSocket
            // implementations
            CIPHER_SUITES_SSLENGINE.remove("TLS_DHE_DSS_WITH_AES_128_CBC_SHA");
            CIPHER_SUITES_SSLENGINE.remove("TLS_DHE_DSS_WITH_AES_256_CBC_SHA");
            CIPHER_SUITES_SSLENGINE.remove("TLS_DHE_RSA_WITH_AES_128_CBC_SHA");
            CIPHER_SUITES_SSLENGINE.remove("TLS_DHE_RSA_WITH_AES_256_CBC_SHA");
            CIPHER_SUITES_SSLENGINE.remove("TLS_DH_anon_WITH_AES_128_CBC_SHA");
            CIPHER_SUITES_SSLENGINE.remove("TLS_DH_anon_WITH_AES_256_CBC_SHA");
            CIPHER_SUITES_SSLENGINE.remove("TLS_RSA_WITH_AES_128_CBC_SHA");
            CIPHER_SUITES_SSLENGINE.remove("TLS_RSA_WITH_AES_256_CBC_SHA");
            // Harmony SSLEngine supports has some older cipher suites
            CIPHER_SUITES_SSLENGINE.add("SSL_DH_DSS_EXPORT_WITH_DES40_CBC_SHA");
            CIPHER_SUITES_SSLENGINE.add("SSL_DH_DSS_WITH_3DES_EDE_CBC_SHA");
            CIPHER_SUITES_SSLENGINE.add("SSL_DH_DSS_WITH_DES_CBC_SHA");
            CIPHER_SUITES_SSLENGINE.add("SSL_DH_RSA_EXPORT_WITH_DES40_CBC_SHA");
            CIPHER_SUITES_SSLENGINE.add("SSL_DH_RSA_WITH_3DES_EDE_CBC_SHA");
            CIPHER_SUITES_SSLENGINE.add("SSL_DH_RSA_WITH_DES_CBC_SHA");
        }
    }

    /**
     * Asserts that the cipher suites array is non-null and that it
     * all of its contents are cipher suites known to this
     * implementation. As a convenience, returns any unenabled cipher
     * suites in a test for those that want to verify separately that
     * all cipher suites were included.
     */
    public static Set<String> assertValidCipherSuites(Set<String> expected, String[] cipherSuites) {
        assertNotNull(cipherSuites);
        assertTrue(cipherSuites.length != 0);

        // Make sure all cipherSuites names are expected
        Set remainingCipherSuites = new HashSet<String>(expected);
        Set unknownCipherSuites = new HashSet<String>();
        for (String cipherSuite : cipherSuites) {
            boolean removed = remainingCipherSuites.remove(cipherSuite);
            if (!removed) {
                unknownCipherSuites.add(cipherSuite);
            }
        }
        assertEquals(Collections.EMPTY_SET, unknownCipherSuites);
        return remainingCipherSuites;
    }

    /**
     * After using assertValidCipherSuites on cipherSuites,
     * assertSupportedCipherSuites additionally verifies that all
     * supported cipher suites where in the input array.
     */
    public static void assertSupportedCipherSuites(Set<String> expected, String[] cipherSuites) {
        Set<String> remainingCipherSuites = assertValidCipherSuites(expected, cipherSuites);
        assertEquals(Collections.EMPTY_SET, remainingCipherSuites);
        assertEquals(expected.size(), cipherSuites.length);
    }

    /**
     * Asserts that the protocols array is non-null and that it all of
     * its contents are protocols known to this implementation. As a
     * convenience, returns any unenabled protocols in a test for
     * those that want to verify separately that all protocols were
     * included.
     */
    public static Set<String> assertValidProtocols(Set<String> expected, String[] protocols) {
        assertNotNull(protocols);
        assertTrue(protocols.length != 0);

        // Make sure all protocols names are expected
        Set remainingProtocols = new HashSet<String>(StandardNames.SSL_SOCKET_PROTOCOLS);
        Set unknownProtocols = new HashSet<String>();
        for (String protocol : protocols) {
            if (!remainingProtocols.remove(protocol)) {
                unknownProtocols.add(protocol);
            }
        }
        assertEquals(Collections.EMPTY_SET, unknownProtocols);
        return remainingProtocols;
    }

    /**
     * After using assertValidProtocols on protocols,
     * assertSupportedProtocols additionally verifies that all
     * supported protocols where in the input array.
     */
    public static void assertSupportedProtocols(Set<String> expected, String[] protocols) {
        Set<String> remainingProtocols = assertValidProtocols(expected, protocols);
        assertEquals(Collections.EMPTY_SET, remainingProtocols);
        assertEquals(expected.size(), protocols.length);
    }
}
