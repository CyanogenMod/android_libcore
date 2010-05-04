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
import java.util.Set;

/**
 * This class defines expected string names for protocols, key types, client and server auth types, cipher suites.
 * Based on documentation from http://java.sun.com/j2se/1.5.0/docs/guide/security/jsse/JSSERefGuide.html#AppA
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

    // removed cipher suites not actually found in RI
    public static final Set<String> CIPHER_SUITES = new HashSet<String>(Arrays.asList(
        "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
        "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
        "SSL_DHE_DSS_WITH_DES_CBC_SHA",
        "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
        "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
        "SSL_DHE_RSA_WITH_DES_CBC_SHA",
        //"SSL_DH_DSS_EXPORT_WITH_DES40_CBC_SHA",
        //"SSL_DH_RSA_EXPORT_WITH_DES40_CBC_SHA",
        "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
        "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
        "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
        "SSL_DH_anon_WITH_DES_CBC_SHA",
        "SSL_DH_anon_WITH_RC4_128_MD5",
        //"SSL_RSA_EXPORT1024_WITH_DES_CBC_SHA",
        //"SSL_RSA_EXPORT1024_WITH_RC4_56_SHA",
        "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
        //"SSL_RSA_EXPORT_WITH_RC2_CBC_40_MD5",
        "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
        "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
        "SSL_RSA_WITH_DES_CBC_SHA",
        "SSL_RSA_WITH_NULL_MD5",
        "SSL_RSA_WITH_NULL_SHA",
        "SSL_RSA_WITH_RC4_128_MD5",
        "SSL_RSA_WITH_RC4_128_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
        //"TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        //"TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_DH_anon_WITH_AES_128_CBC_SHA",
        //"TLS_DH_anon_WITH_AES_256_CBC_SHA",
        "TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5",
        "TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA",
        //"TLS_KRB5_EXPORT_WITH_RC2_CBC_40_MD5",
        //"TLS_KRB5_EXPORT_WITH_RC2_CBC_40_SHA",
        "TLS_KRB5_EXPORT_WITH_RC4_40_MD5",
        "TLS_KRB5_EXPORT_WITH_RC4_40_SHA",
        "TLS_KRB5_WITH_3DES_EDE_CBC_MD5",
        "TLS_KRB5_WITH_3DES_EDE_CBC_SHA",
        "TLS_KRB5_WITH_DES_CBC_MD5",
        "TLS_KRB5_WITH_DES_CBC_SHA",
        "TLS_KRB5_WITH_RC4_128_MD5",
        "TLS_KRB5_WITH_RC4_128_SHA",
        "TLS_RSA_WITH_AES_128_CBC_SHA"));
        //"TLS_RSA_WITH_AES_256_CBC_SHA"));
}
