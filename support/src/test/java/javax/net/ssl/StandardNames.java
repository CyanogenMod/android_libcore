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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import junit.framework.Assert;

/**
 * This class defines expected string names for protocols, key types,
 * client and server auth types, cipher suites.
 *
 * Initially based on "Appendix A: Standard Names" of
 * <a href="http://java.sun.com/j2se/1.5.0/docs/guide/security/jsse/JSSERefGuide.html#AppA">
 * Java &trade; Secure Socket Extension (JSSE) Reference Guide
 * for the Java &trade; 2 Platform Standard Edition 5
 * </a>.
 *
 * Updated based on the
 * <a href="http://java.sun.com/javase/6/docs/technotes/guides/security/SunProviders.html">
 * Java &trade; Cryptography Architecture Sun Providers Documentation
 * for Java &trade; Platform Standard Edition 6
 * </a>.
 * See also the
 * <a href="http://java.sun.com/javase/6/docs/technotes/guides/security/StandardNames.html">
 * Java &trade; Cryptography Architecture Standard Algorithm Name Documentation
 * </a>.
 */
public final class StandardNames extends Assert {

    /**
     * A map from algorithm type (e.g. Cipher) to a set of algorithms (e.g. AES, DES, ...)
     */
    public static final Map<String,Set<String>> PROVIDER_ALGORITHMS
            = new HashMap<String,Set<String>>();
    private static void provide(String type, String algorithm) {
        Set<String> algorithms = PROVIDER_ALGORITHMS.get(type);
        if (algorithms == null) {
            algorithms = new HashSet();
            PROVIDER_ALGORITHMS.put(type, algorithms);
        }
        assertTrue("Duplicate " + type + " " + algorithm,
                   algorithms.add(algorithm.toUpperCase()));
    }
    private static void unprovide(String type, String algorithm) {
        Set<String> algorithms = PROVIDER_ALGORITHMS.get(type);
        assertNotNull(algorithms);
        assertTrue(algorithm, algorithms.remove(algorithm.toUpperCase()));
        if (algorithms.isEmpty()) {
            assertNotNull(PROVIDER_ALGORITHMS.remove(type));
        }
    }
    static {
        provide("AlgorithmParameterGenerator", "DSA");
        provide("AlgorithmParameterGenerator", "DiffieHellman");
        provide("AlgorithmParameters", "AES");
        provide("AlgorithmParameters", "Blowfish");
        provide("AlgorithmParameters", "DES");
        provide("AlgorithmParameters", "DESede");
        provide("AlgorithmParameters", "DSA");
        provide("AlgorithmParameters", "DiffieHellman");
        provide("AlgorithmParameters", "OAEP");
        provide("AlgorithmParameters", "PBEWithMD5AndDES");
        provide("AlgorithmParameters", "PBEWithMD5AndTripleDES");
        provide("AlgorithmParameters", "PBEWithSHA1AndDESede");
        provide("AlgorithmParameters", "PBEWithSHA1AndRC2_40");
        provide("AlgorithmParameters", "RC2");
        provide("CertPathBuilder", "PKIX");
        provide("CertPathValidator", "PKIX");
        provide("CertStore", "Collection");
        provide("CertStore", "LDAP");
        provide("CertificateFactory", "X.509");
        provide("Cipher", "AES");
        provide("Cipher", "AESWrap");
        provide("Cipher", "ARCFOUR");
        provide("Cipher", "Blowfish");
        provide("Cipher", "DES");
        provide("Cipher", "DESede");
        provide("Cipher", "DESedeWrap");
        provide("Cipher", "PBEWithMD5AndDES");
        provide("Cipher", "PBEWithMD5AndTripleDES");
        provide("Cipher", "PBEWithSHA1AndDESede");
        provide("Cipher", "PBEWithSHA1AndRC2_40");
        provide("Cipher", "RC2");
        provide("Cipher", "RSA");
        provide("Configuration", "JavaLoginConfig");
        provide("KeyAgreement", "DiffieHellman");
        provide("KeyFactory", "DSA");
        provide("KeyFactory", "DiffieHellman");
        provide("KeyFactory", "RSA");
        provide("KeyGenerator", "AES");
        provide("KeyGenerator", "ARCFOUR");
        provide("KeyGenerator", "Blowfish");
        provide("KeyGenerator", "DES");
        provide("KeyGenerator", "DESede");
        provide("KeyGenerator", "HmacMD5");
        provide("KeyGenerator", "HmacSHA1");
        provide("KeyGenerator", "HmacSHA256");
        provide("KeyGenerator", "HmacSHA384");
        provide("KeyGenerator", "HmacSHA512");
        provide("KeyGenerator", "RC2");
        provide("KeyInfoFactory", "DOM");
        provide("KeyManagerFactory", "SunX509");
        provide("KeyPairGenerator", "DSA");
        provide("KeyPairGenerator", "DiffieHellman");
        provide("KeyPairGenerator", "RSA");
        provide("KeyStore", "JCEKS");
        provide("KeyStore", "JKS");
        provide("KeyStore", "PKCS12");
        provide("Mac", "HmacMD5");
        provide("Mac", "HmacSHA1");
        provide("Mac", "HmacSHA256");
        provide("Mac", "HmacSHA384");
        provide("Mac", "HmacSHA512");
        provide("MessageDigest", "MD2");
        provide("MessageDigest", "MD5");
        provide("MessageDigest", "SHA-256");
        provide("MessageDigest", "SHA-384");
        provide("MessageDigest", "SHA-512");
        provide("Policy", "JavaPolicy");
        provide("SSLContext", "SSLv3");
        provide("SSLContext", "TLSv1");
        provide("SecretKeyFactory", "DES");
        provide("SecretKeyFactory", "DESede");
        provide("SecretKeyFactory", "PBEWithMD5AndDES");
        provide("SecretKeyFactory", "PBEWithMD5AndTripleDES");
        provide("SecretKeyFactory", "PBEWithSHA1AndDESede");
        provide("SecretKeyFactory", "PBEWithSHA1AndRC2_40");
        provide("SecretKeyFactory", "PBKDF2WithHmacSHA1");
        provide("SecureRandom", "SHA1PRNG");
        provide("Signature", "MD2withRSA");
        provide("Signature", "MD5withRSA");
        provide("Signature", "NONEwithDSA");
        provide("Signature", "SHA1withDSA");
        provide("Signature", "SHA1withRSA");
        provide("Signature", "SHA256withRSA");
        provide("Signature", "SHA384withRSA");
        provide("Signature", "SHA512withRSA");
        provide("TerminalFactory", "PC/SC");
        provide("TransformService", "http://www.w3.org/2000/09/xmldsig#base64");
        provide("TransformService", "http://www.w3.org/2000/09/xmldsig#enveloped-signature");
        provide("TransformService", "http://www.w3.org/2001/10/xml-exc-c14n#");
        provide("TransformService", "http://www.w3.org/2001/10/xml-exc-c14n#WithComments");
        provide("TransformService", "http://www.w3.org/2002/06/xmldsig-filter2");
        provide("TransformService", "http://www.w3.org/TR/1999/REC-xpath-19991116");
        provide("TransformService", "http://www.w3.org/TR/1999/REC-xslt-19991116");
        provide("TransformService", "http://www.w3.org/TR/2001/REC-xml-c14n-20010315");
        provide("TransformService", "http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments");
        provide("TrustManagerFactory", "PKIX");
        provide("XMLSignatureFactory", "DOM");

        // Not clearly documented by RI
        provide("GssApiMechanism", "1.2.840.113554.1.2.2");
        provide("GssApiMechanism", "1.3.6.1.5.5.2");

        // Not correctly documented by RI which left off the Factory suffix
        provide("SaslClientFactory", "CRAM-MD5");
        provide("SaslClientFactory", "DIGEST-MD5");
        provide("SaslClientFactory", "EXTERNAL");
        provide("SaslClientFactory", "GSSAPI");
        provide("SaslClientFactory", "PLAIN");
        provide("SaslServerFactory", "CRAM-MD5");
        provide("SaslServerFactory", "DIGEST-MD5");
        provide("SaslServerFactory", "GSSAPI");

        // Documentation seems to list alias instead of actual name
        // provide("MessageDigest", "SHA-1");
        provide("MessageDigest", "SHA");

        // Mentioned in javadoc, not documentation
        provide("SSLContext", "Default");

        // Not documented as in RI 6 but mentioned in Standard Names
        provide("AlgorithmParameters", "PBE");
        provide("SSLContext", "SSL");
        provide("SSLContext", "TLS");

        // Not documented as in RI 6 but that exist in RI 6
        if (TestSSLContext.IS_RI) {
            provide("CertStore", "com.sun.security.IndexedCollection");
            provide("KeyGenerator", "SunTlsKeyMaterial");
            provide("KeyGenerator", "SunTlsMasterSecret");
            provide("KeyGenerator", "SunTlsPrf");
            provide("KeyGenerator", "SunTlsRsaPremasterSecret");
            provide("KeyManagerFactory", "NewSunX509");
            provide("KeyStore", "CaseExactJKS");
            provide("Mac", "HmacPBESHA1");
            provide("Mac", "SslMacMD5");
            provide("Mac", "SslMacSHA1");
            provide("SecureRandom", "NativePRNG");
            provide("Signature", "MD5andSHA1withRSA");
            provide("TrustManagerFactory", "SunX509");
        }

        // Fixups for dalvik
        if (!TestSSLContext.IS_RI) {

            // whole types that we do not provide
            PROVIDER_ALGORITHMS.remove("Configuration");
            PROVIDER_ALGORITHMS.remove("GssApiMechanism");
            PROVIDER_ALGORITHMS.remove("KeyInfoFactory");
            PROVIDER_ALGORITHMS.remove("Policy");
            PROVIDER_ALGORITHMS.remove("SaslClientFactory");
            PROVIDER_ALGORITHMS.remove("SaslServerFactory");
            PROVIDER_ALGORITHMS.remove("TerminalFactory");
            PROVIDER_ALGORITHMS.remove("TransformService");
            PROVIDER_ALGORITHMS.remove("XMLSignatureFactory");

            // different names
            unprovide("AlgorithmParameterGenerator", "DiffieHellman");
            provide("AlgorithmParameterGenerator", "DH");

            unprovide("AlgorithmParameters", "DiffieHellman");
            provide("AlgorithmParameters", "DH");

            unprovide("CertificateFactory", "X.509");
            provide("CertificateFactory", "X509");

            unprovide("Cipher", "PBEWithSHA1AndRC2_40");
            provide("Cipher", "PBEWithSHAAnd40BitRC2-CBC");

            unprovide("KeyAgreement", "DiffieHellman");
            provide("KeyAgreement", "DH");

            unprovide("KeyFactory", "DiffieHellman");
            provide("KeyFactory", "DH");

            unprovide("KeyManagerFactory", "SunX509");
            provide("KeyManagerFactory", "X509");

            unprovide("KeyPairGenerator", "DiffieHellman");
            provide("KeyPairGenerator", "DH");

            unprovide("MessageDigest", "SHA");
            provide("MessageDigest", "SHA-1");

            unprovide("SecretKeyFactory", "PBEWithSHA1AndRC2_40");
            provide("SecretKeyFactory", "PBEWithSHAAnd40BitRC2-CBC");

            unprovide("Signature", "MD5withRSA");
            provide("Signature", "MD5WithRSAEncryption");
            unprovide("Signature", "SHA1withRSA");
            provide("Signature", "SHA1WithRSAEncryption");
            unprovide("Signature", "SHA256WithRSA");
            provide("Signature", "SHA256WithRSAEncryption");
            unprovide("Signature", "SHA384WithRSA");
            provide("Signature", "SHA384WithRSAEncryption");
            unprovide("Signature", "SHA512WithRSA");
            provide("Signature", "SHA512WithRSAEncryption");

            // dropped the Sun prefix
            provide("TrustManagerFactory", "X509");

            // extra noise
            provide("AlgorithmParameterGenerator", "1.2.840.113549.3.7");
            provide("AlgorithmParameterGenerator", "1.3.14.3.2.7");
            provide("AlgorithmParameterGenerator", "AES");
            provide("AlgorithmParameterGenerator", "DES");
            provide("AlgorithmParameterGenerator", "DESede");
            provide("AlgorithmParameters", "1.2.840.113549.3.7");
            provide("AlgorithmParameters", "IES");
            provide("AlgorithmParameters", "PKCS12PBE");
            provide("AlgorithmParameters", "PSS");
            provide("CertificateFactory", "X.509");
            provide("Cipher", "1.2.840.113549.1.1.1");
            provide("Cipher", "1.2.840.113549.1.1.7");
            provide("Cipher", "1.2.840.113549.1.9.16.3.6");
            provide("Cipher", "1.2.840.113549.3.7");
            provide("Cipher", "1.3.14.3.2.7");
            provide("Cipher", "2.16.840.1.101.3.4.1.1");
            provide("Cipher", "2.16.840.1.101.3.4.1.2");
            provide("Cipher", "2.16.840.1.101.3.4.1.21");
            provide("Cipher", "2.16.840.1.101.3.4.1.22");
            provide("Cipher", "2.16.840.1.101.3.4.1.23");
            provide("Cipher", "2.16.840.1.101.3.4.1.24");
            provide("Cipher", "2.16.840.1.101.3.4.1.3");
            provide("Cipher", "2.16.840.1.101.3.4.1.4");
            provide("Cipher", "2.16.840.1.101.3.4.1.41");
            provide("Cipher", "2.16.840.1.101.3.4.1.42");
            provide("Cipher", "2.16.840.1.101.3.4.1.43");
            provide("Cipher", "2.16.840.1.101.3.4.1.44");
            provide("Cipher", "2.5.8.1.1");
            provide("Cipher", "BrokenPBEWithMD5AndDES");
            provide("Cipher", "BrokenPBEWithSHA1AndDES");
            provide("Cipher", "BrokenPBEWithSHAAnd2-KEYTripleDES-CBC");
            provide("Cipher", "BrokenPBEWithSHAAnd3-KEYTripleDES-CBC");
            provide("Cipher", "BrokenIES");
            provide("Cipher", "IES");
            provide("Cipher", "OldPBEWithSHAAnd3-KEYTripleDES-CBC");
            provide("Cipher", "PBEWithMD5And128BitAES-CBC-OpenSSL");
            provide("Cipher", "PBEWithMD5And192BitAES-CBC-OpenSSL");
            provide("Cipher", "PBEWithMD5And256BitAES-CBC-OpenSSL");
            provide("Cipher", "PBEWithMD5AndRC2");
            provide("Cipher", "PBEWithSHA1AndDES");
            provide("Cipher", "PBEWithSHA256And128BitAES-CBC-BC");
            provide("Cipher", "PBEWithSHA256And192BitAES-CBC-BC");
            provide("Cipher", "PBEWithSHA256And256BitAES-CBC-BC");
            provide("Cipher", "PBEWithSHAAnd128BitAES-CBC-BC");
            provide("Cipher", "PBEWithSHAAnd192BitAES-CBC-BC");
            provide("Cipher", "PBEWithSHAAnd2-KEYTripleDES-CBC");
            provide("Cipher", "PBEWithSHAAnd256BitAES-CBC-BC");
            provide("Cipher", "PBEWithSHAAnd3-KEYTripleDES-CBC");
            provide("Cipher", "RSA/1");
            provide("Cipher", "RSA/2");
            provide("Cipher", "RSA/ISO9796-1");
            provide("Cipher", "RSA/OAEP");
            provide("Cipher", "RSA/PKCS1");
            provide("Cipher", "RSA/RAW");
            provide("KeyFactory", "X.509");
            provide("KeyGenerator", "1.2.840.113549.3.7");
            provide("KeyGenerator", "2.16.840.1.101.3.4.1.1");
            provide("KeyGenerator", "2.16.840.1.101.3.4.1.2");
            provide("KeyGenerator", "2.16.840.1.101.3.4.1.21");
            provide("KeyGenerator", "2.16.840.1.101.3.4.1.22");
            provide("KeyGenerator", "2.16.840.1.101.3.4.1.23");
            provide("KeyGenerator", "2.16.840.1.101.3.4.1.24");
            provide("KeyGenerator", "2.16.840.1.101.3.4.1.25");
            provide("KeyGenerator", "2.16.840.1.101.3.4.1.3");
            provide("KeyGenerator", "2.16.840.1.101.3.4.1.4");
            provide("KeyGenerator", "2.16.840.1.101.3.4.1.41");
            provide("KeyGenerator", "2.16.840.1.101.3.4.1.42");
            provide("KeyGenerator", "2.16.840.1.101.3.4.1.43");
            provide("KeyGenerator", "2.16.840.1.101.3.4.1.44");
            provide("KeyGenerator", "2.16.840.1.101.3.4.1.45");
            provide("KeyGenerator", "2.16.840.1.101.3.4.1.5");
            provide("KeyGenerator", "2.16.840.1.101.3.4.2");
            provide("KeyGenerator", "2.16.840.1.101.3.4.22");
            provide("KeyGenerator", "2.16.840.1.101.3.4.42");
            provide("KeyGenerator", "AESWrap");
            provide("KeyGenerator", "DESedeWrap");
            provide("KeyGenerator", "HmacSHA224");
            provide("KeyStore", "BCPKCS12");
            provide("KeyStore", "BKS");
            provide("KeyStore", "BouncyCastle");
            provide("KeyStore", "PKCS12-DEF");
            provide("Mac", "DESedeMAC");
            provide("Mac", "DESedeMAC/CFB8");
            provide("Mac", "DESedeMAC64");
            provide("Mac", "DESMAC");
            provide("Mac", "DESMAC/CFB8");
            provide("Mac", "DESWithISO9797");
            provide("Mac", "HmacSHA224");
            provide("Mac", "ISO9797ALG3MAC");
            provide("Mac", "PBEWithHmacSHA");
            provide("Mac", "PBEWithHmacSHA1");
            provide("MessageDigest", "SHA-224");
            provide("SecretKeyFactory", "PBEWithHmacSHA1");
            provide("SecretKeyFactory", "PBEWithMD5And128BitAES-CBC-OpenSSL");
            provide("SecretKeyFactory", "PBEWithMD5And192BitAES-CBC-OpenSSL");
            provide("SecretKeyFactory", "PBEWithMD5And256BitAES-CBC-OpenSSL");
            provide("SecretKeyFactory", "PBEWithMD5AndRC2");
            provide("SecretKeyFactory", "PBEWithSHA1AndDES");
            provide("SecretKeyFactory", "PBEWithSHA1AndRC2");
            provide("SecretKeyFactory", "PBEWithSHA256And128BitAES-CBC-BC");
            provide("SecretKeyFactory", "PBEWithSHA256And192BitAES-CBC-BC");
            provide("SecretKeyFactory", "PBEWithSHA256And256BitAES-CBC-BC");
            provide("SecretKeyFactory", "PBEWithSHAAnd128BitAES-CBC-BC");
            provide("SecretKeyFactory", "PBEWithSHAAnd192BitAES-CBC-BC");
            provide("SecretKeyFactory", "PBEWithSHAAnd2-KEYTripleDES-CBC");
            provide("SecretKeyFactory", "PBEWithSHAAnd256BitAES-CBC-BC");
            provide("SecretKeyFactory", "PBEWithSHAAnd3-KEYTripleDES-CBC");
            provide("Signature", "1.2.840.113549.1.1.10");
            provide("Signature", "DSA");
            provide("Signature", "MD4WithRSAEncryption");
            provide("Signature", "MD5withRSA/ISO9796-2");
            provide("Signature", "RSASSA-PSS");
            provide("Signature", "SHA1withRSA/ISO9796-2");
            provide("Signature", "SHA1withRSA/PSS");
            provide("Signature", "SHA224WithRSAEncryption");
            provide("Signature", "SHA224withRSA/PSS");
            provide("Signature", "SHA256withRSA/PSS");
            provide("Signature", "SHA384withRSA/PSS");
            provide("Signature", "SHA512withRSA/PSS");

            // missing
            unprovide("AlgorithmParameters", "Blowfish");
            unprovide("AlgorithmParameters", "PBE");
            unprovide("AlgorithmParameters", "PBEWithMD5AndDES");
            unprovide("AlgorithmParameters", "PBEWithMD5AndTripleDES");
            unprovide("AlgorithmParameters", "PBEWithSHA1AndDESede");
            unprovide("AlgorithmParameters", "PBEWithSHA1AndRC2_40");
            unprovide("AlgorithmParameters", "RC2");
            unprovide("CertStore", "LDAP");
            unprovide("Cipher", "ARCFOUR");
            unprovide("Cipher", "Blowfish");
            unprovide("Cipher", "PBEWithMD5AndTripleDES");
            unprovide("Cipher", "PBEWithSHA1AndDESede");
            unprovide("Cipher", "RC2");
            unprovide("KeyGenerator", "ARCFOUR");
            unprovide("KeyGenerator", "Blowfish");
            unprovide("KeyGenerator", "RC2");
            unprovide("KeyStore", "JCEKS");
            unprovide("KeyStore", "JKS");
            unprovide("MessageDigest", "MD2");
            unprovide("SecretKeyFactory", "PBEWithMD5AndTripleDES");
            unprovide("SecretKeyFactory", "PBEWithSHA1AndDESede");
            unprovide("SecretKeyFactory", "PBKDF2WithHmacSHA1");
            unprovide("SSLContext", "SSLv3");
            unprovide("SSLContext", "TLSv1");
            unprovide("Signature", "MD2withRSA");
            unprovide("TrustManagerFactory", "PKIX");
        }
    }

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
