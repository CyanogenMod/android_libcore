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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This class defines expected string names for protocols, key types, client and server auth types, cipher suites.
 * 
 * Based on documentation from http://java.sun.com/j2se/1.5.0/docs/guide/security/jsse/JSSERefGuide.html#AppA
 *
 * Java 6 version http://java.sun.com/javase/6/docs/technotes/guides/security/SunProviders.html
 */
public final class StandardNames {

    public static final Set<String> SSL_CONTEXT_PROTOCOLS = new HashSet<String>(Arrays.asList(
        "SSL",
        "SSLv2",
        "SSLv3",
        "TLS",
        "TLSv1"));

    public static final Set<String> KEY_TYPES = new HashSet<String>(Arrays.asList(
        "RSA",
        "DSA",
        "DH_RSA",
        "DH_DSA"));

    public static final Set<String> SSL_SOCKET_PROTOCOLS = new HashSet<String>(Arrays.asList(
        "SSLv2",
        "SSLv3",
        "TLSv1",
        "SSLv2Hello"));

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

    static {
        // Note these are added in priority order as defined by RI 6 documentation.
        addBoth(   "SSL_RSA_WITH_RC4_128_MD5");
        addBoth(   "SSL_RSA_WITH_RC4_128_SHA");
        addBoth(   "TLS_RSA_WITH_AES_128_CBC_SHA");
        addOpenSsl("TLS_RSA_WITH_AES_256_CBC_SHA");
        addOpenSsl("TLS_ECDH_ECDSA_WITH_RC4_128_SHA");
        addOpenSsl("TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA");
        addOpenSsl("TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA");
        addOpenSsl("TLS_ECDH_RSA_WITH_RC4_128_SHA");
        addOpenSsl("TLS_ECDH_RSA_WITH_AES_128_CBC_SHA");
        addOpenSsl("TLS_ECDH_RSA_WITH_AES_256_CBC_SHA");
        addOpenSsl("TLS_ECDHE_ECDSA_WITH_RC4_128_SHA");
        addOpenSsl("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA");
        addOpenSsl("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA");
        addOpenSsl("TLS_ECDHE_RSA_WITH_RC4_128_SHA");
        addOpenSsl("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
        addOpenSsl("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA");
        addBoth(   "TLS_DHE_RSA_WITH_AES_128_CBC_SHA");
        addOpenSsl("TLS_DHE_RSA_WITH_AES_256_CBC_SHA");
        addBoth(   "TLS_DHE_DSS_WITH_AES_128_CBC_SHA");
        addOpenSsl("TLS_DHE_DSS_WITH_AES_256_CBC_SHA");
        addBoth(   "SSL_RSA_WITH_3DES_EDE_CBC_SHA");
        addOpenSsl("TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA");
        addOpenSsl("TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA");
        addOpenSsl("TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA");
        addOpenSsl("TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA");
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
        addOpenSsl("TLS_ECDH_ECDSA_WITH_NULL_SHA");
        addOpenSsl("TLS_ECDH_RSA_WITH_NULL_SHA");
        addOpenSsl("TLS_ECDHE_ECDSA_WITH_NULL_SHA");
        addOpenSsl("TLS_ECDHE_RSA_WITH_NULL_SHA");
        addBoth(   "SSL_DH_anon_WITH_RC4_128_MD5");
        addBoth(   "TLS_DH_anon_WITH_AES_128_CBC_SHA");
        addOpenSsl("TLS_DH_anon_WITH_AES_256_CBC_SHA");
        addBoth(   "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA");
        addBoth(   "SSL_DH_anon_WITH_DES_CBC_SHA");
        addOpenSsl("TLS_ECDH_anon_WITH_RC4_128_SHA");
        addOpenSsl("TLS_ECDH_anon_WITH_AES_128_CBC_SHA");
        addOpenSsl("TLS_ECDH_anon_WITH_AES_256_CBC_SHA");
        addOpenSsl("TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA");
        addBoth(   "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5");
        addBoth(   "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA");
        addOpenSsl("TLS_ECDH_anon_WITH_NULL_SHA");

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
        CIPHER_SUITES_NEITHER.add("SSL_DH_DSS_EXPORT_WITH_DES40_CBC_SHA");
        CIPHER_SUITES_NEITHER.add("SSL_DH_RSA_EXPORT_WITH_DES40_CBC_SHA");

        // Old non standard exportable encryption
        CIPHER_SUITES_NEITHER.add("SSL_RSA_EXPORT1024_WITH_DES_CBC_SHA");
        CIPHER_SUITES_NEITHER.add("SSL_RSA_EXPORT1024_WITH_RC4_56_SHA");
        
        // No RC2
        CIPHER_SUITES_NEITHER.add("SSL_RSA_EXPORT_WITH_RC2_CBC_40_MD5");
        CIPHER_SUITES_NEITHER.add("TLS_KRB5_EXPORT_WITH_RC2_CBC_40_SHA");
        CIPHER_SUITES_NEITHER.add("TLS_KRB5_EXPORT_WITH_RC2_CBC_40_MD5");
        
        CIPHER_SUITES = (TestSSLContext.IS_RI) ? CIPHER_SUITES_RI : CIPHER_SUITES_OPENSSL;
    }
}
