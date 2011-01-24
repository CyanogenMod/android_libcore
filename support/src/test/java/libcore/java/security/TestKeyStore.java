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

package libcore.java.security;

import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import junit.framework.Assert;
import libcore.javax.net.ssl.TestKeyManager;
import libcore.javax.net.ssl.TestTrustManager;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

/**
 * TestKeyStore is a convenience class for other tests that
 * want a canned KeyStore with a variety of key pairs.
 *
 * Creating a key store is relatively slow, so a singleton instance is
 * accessible via TestKeyStore.get().
 */
public final class TestKeyStore extends Assert {

    static {
        if (StandardNames.IS_RI) {
            // Needed to create BKS keystore but add at end so most
            // algorithm come from the default providers
            Security.insertProviderAt(new BouncyCastleProvider(),
                                      Security.getProviders().length+1);
        }
    }
    private static final boolean TEST_MANAGERS = true;

    public final KeyStore keyStore;
    public final char[] storePassword;
    public final char[] keyPassword;
    public final KeyManager[] keyManagers;
    public final TrustManager[] trustManagers;
    public final TestKeyManager keyManager;
    public final TestTrustManager trustManager;

    private TestKeyStore(KeyStore keyStore,
                         char[] storePassword,
                         char[] keyPassword) {
        this.keyStore = keyStore;
        this.storePassword = storePassword;
        this.keyPassword = keyPassword;
        this.keyManagers = createKeyManagers(keyStore, storePassword);
        this.trustManagers = createTrustManagers(keyStore);
        this.keyManager = (TestKeyManager)keyManagers[0];
        this.trustManager = (TestTrustManager)trustManagers[0];
    }

    public static KeyManager[] createKeyManagers(final KeyStore keyStore,
                                                 final char[] storePassword) {
        try {
            String kmfa = KeyManagerFactory.getDefaultAlgorithm();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(kmfa);
            kmf.init(keyStore, storePassword);
            return TestKeyManager.wrap(kmf.getKeyManagers());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TrustManager[] createTrustManagers(final KeyStore keyStore) {
        try {
            String tmfa = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfa);
            tmf.init(keyStore);
            return TestTrustManager.wrap(tmf.getTrustManagers());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final TestKeyStore ROOT_CA
            = create(new String[] { "RSA" },
                     null,
                     null,
                     "RootCA",
                     x509Principal("Test Root Certificate Authority"),
                     0,
                     true,
                     null,
                     null);
    private static final TestKeyStore INTERMEDIATE_CA
            = create(new String[] { "RSA" },
                     null,
                     null,
                     "IntermediateCA",
                     x509Principal("Test Intermediate Certificate Authority"),
                     0,
                     true,
                     ROOT_CA.getPrivateKey("RSA", "RSA"),
                     ROOT_CA.getRootCertificate("RSA"));
    private static final TestKeyStore SERVER
            = create(new String[] { "RSA" },
                     null,
                     null,
                     "server",
                     localhost(),
                     0,
                     false,
                     INTERMEDIATE_CA.getPrivateKey("RSA", "RSA"),
                     INTERMEDIATE_CA.getRootCertificate("RSA"));
    private static final TestKeyStore CLIENT
            = new TestKeyStore(createClient(INTERMEDIATE_CA.keyStore), null, null);
    private static final TestKeyStore CLIENT_CERTIFICATE
            = create(new String[] { "RSA" },
                     null,
                     null,
                     "client",
                     x509Principal("test@user"),
                     0,
                     false,
                     INTERMEDIATE_CA.getPrivateKey("RSA", "RSA"),
                     INTERMEDIATE_CA.getRootCertificate("RSA"));

    private static final TestKeyStore ROOT_CA_2
            = create(new String[] { "RSA" },
                     null,
                     null,
                     "RootCA2",
                     x509Principal("Test Root Certificate Authority 2"),
                     0,
                     true,
                     null,
                     null);
    private static final TestKeyStore CLIENT_2
            = new TestKeyStore(createClient(ROOT_CA_2.keyStore), null, null);

    /**
     * Return a server keystore with a matched RSA certificate and
     * private key as well as a CA certificate.
     */
    public static TestKeyStore getServer() {
        return SERVER;
    }

    /**
     * Return a keystore with a CA certificate
     */
    public static TestKeyStore getClient() {
        return CLIENT;
    }

    /**
     * Return a client keystore with a matched RSA certificate and
     * private key as well as a CA certificate.
     */
    public static TestKeyStore getClientCertificate() {
        return CLIENT_CERTIFICATE;
    }

    /**
     * Return a keystore with a second CA certificate that does not
     * trust the server certificate returned by getServer for negative
     * testing.
     */
    public static TestKeyStore getClientCA2() {
        return CLIENT_2;
    }

    /**
     * Create a new KeyStore containing the requested key types.
     * Since key generation can be expensive, most tests should reuse
     * the RSA-only singleton instance returned by TestKeyStore.get
     *
     * @param keyAlgorithms The requested key types to generate and include
     * @param keyStorePassword Password used to protect the private key
     * @param aliasPrefix A unique prefix to identify the key aliases
     * @param keyUsage {@link KeyUsage} bit mask for 2.5.29.15 extension
     * @param ca true If the keys being created are for a CA
     * @param signer If non-null, a private key entry to be used for signing, otherwise self-sign
     * @param signer If non-null, a root CA to include in the final store
     */
    public static TestKeyStore create(String[] keyAlgorithms,
                                      char[] storePassword,
                                      char[] keyPassword,
                                      String aliasPrefix,
                                      X509Principal subject,
                                      int keyUsage,
                                      boolean ca,
                                      PrivateKeyEntry signer,
                                      Certificate rootCa) {
        try {
            if (StandardNames.IS_RI) {
                // JKS does not allow null password
                if (storePassword == null) {
                    storePassword = "password".toCharArray();
                }
                if (keyPassword == null) {
                    keyPassword = "password".toCharArray();
                }
            }
            KeyStore keyStore = createKeyStore();
            boolean ecRsa = false;
            for (String keyAlgorithm : keyAlgorithms) {
                String publicAlias  = aliasPrefix + "-public-"  + keyAlgorithm;
                String privateAlias = aliasPrefix + "-private-" + keyAlgorithm;
                if (keyAlgorithm.equals("EC_RSA") && signer == null && rootCa == null) {
                    createKeys(keyStore, keyPassword,
                               keyAlgorithm,
                               publicAlias, privateAlias,
                               subject,
                               keyUsage,
                               ca,
                               privateKey(keyStore, keyPassword, "RSA", "RSA"));
                    continue;
                }
                createKeys(keyStore, keyPassword,
                           keyAlgorithm,
                           publicAlias, privateAlias,
                           subject,
                           keyUsage,
                           ca,
                           signer);
            }
            if (rootCa != null) {
                keyStore.setCertificateEntry(aliasPrefix
                                             + "-root-ca-"
                                             + rootCa.getPublicKey().getAlgorithm(),
                                             rootCa);
            }
            return new TestKeyStore(keyStore, storePassword, keyPassword);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the key algorithm for a possible compound algorithm
     * identifier containing an underscore. If not underscore is
     * present, the argument is returned unmodified. However for an
     * algorithm such as EC_RSA, return EC.
     */
    public static String keyAlgorithm(String algorithm) {
        int index = algorithm.indexOf('_');
        if (index == -1) {
            return algorithm;
        }
        return algorithm.substring(0, index);
    }


    /**
     * Return the signature algorithm for a possible compound
     * algorithm identifier containing an underscore. If not
     * underscore is present, the argument is returned
     * unmodified. However for an algorithm such as EC_RSA, return
     * RSA.
     */
    public static String signatureAlgorithm(String algorithm) {
        int index = algorithm.indexOf('_');
        if (index == -1) {
            return algorithm;
        }
        return algorithm.substring(index+1, algorithm.length());
    }

    /**
     * Create an empty KeyStore
     *
     * The KeyStore is optionally password protected by the
     * keyStorePassword argument, which can be null if a password is
     * not desired.
     */
    public static KeyStore createKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(StandardNames.KEY_STORE_ALGORITHM);
        keyStore.load(null, null);
        return keyStore;
    }

    /**
     * Add newly generated keys of a given key type to an existing
     * KeyStore. The PrivateKey will be stored under the specified
     * private alias name. The X509Certificate will be stored on the
     * public alias name and have the given subject distiguished
     * name.
     *
     * If a CA is provided, it will be used to sign the generated
     * certificate. Otherwise, the certificate will be self
     * signed. The certificate will be valid for one day before and
     * one day after the time of creation.
     *
     * Based on:
     * org.bouncycastle.jce.provider.test.SigTest
     * org.bouncycastle.jce.provider.test.CertTest
     */
    public static KeyStore createKeys(KeyStore keyStore,
                                      char[] keyPassword,
                                      String keyAlgorithm,
                                      String publicAlias,
                                      String privateAlias,
                                      X509Principal subject,
                                      int keyUsage,
                                      boolean ca,
                                      PrivateKeyEntry signer) throws Exception {
        PrivateKey caKey;
        X509Certificate caCert;
        X509Certificate[] caCertChain;
        if (signer == null) {
            caKey = null;
            caCert = null;
            caCertChain = null;
        } else {
            caKey = signer.getPrivateKey();
            caCert = (X509Certificate)signer.getCertificate();
            caCertChain = (X509Certificate[])signer.getCertificateChain();
        }

        PrivateKey privateKey;
        X509Certificate x509c;
        if (publicAlias == null && privateAlias == null) {
            // don't want anything apparently
            privateKey = null;
            x509c = null;
        } else {
            // 1.) we make the keys
            int keySize;
            String signatureAlgorithm;
            if (keyAlgorithm.equals("RSA")) {
                keySize = StandardNames.IS_RI ? 1024 : 512; // 512 breaks SSL_RSA_EXPORT_* on RI
                signatureAlgorithm = "sha1WithRSA";
            } else if (keyAlgorithm.equals("DSA")) {
                keySize = 512;
                signatureAlgorithm = "sha1WithDSA";
            } else if (keyAlgorithm.equals("EC")) {
                keySize = 256;
                signatureAlgorithm = "sha1WithECDSA";
            } else if (keyAlgorithm.equals("EC_RSA")) {
                keySize = 256;
                keyAlgorithm = "EC";
                signatureAlgorithm = "sha1WithRSA";
            } else {
                throw new IllegalArgumentException("Unknown key algorithm " + keyAlgorithm);
            }

            KeyPairGenerator kpg = KeyPairGenerator.getInstance(keyAlgorithm);
            kpg.initialize(keySize, new SecureRandom());

            KeyPair kp = kpg.generateKeyPair();
            privateKey = (PrivateKey)kp.getPrivate();
            PublicKey publicKey  = (PublicKey)kp.getPublic();
            // 2.) use keys to make certficate

            // note that there doesn't seem to be a standard way to make a
            // certificate using java.* or javax.*. The CertificateFactory
            // interface assumes you want to read in a stream of bytes a
            // factory specific format. So here we use Bouncy Castle's
            // X509V3CertificateGenerator and related classes.
            X509Principal issuer;
            if (caCert == null) {
                issuer = subject;
            } else {
                Principal xp = caCert.getSubjectDN();
                issuer = new X509Principal(new X509Name(xp.getName()));
            }

            long millisPerDay = 24 * 60 * 60 * 1000;
            long now = System.currentTimeMillis();
            Date start = new Date(now - millisPerDay);
            Date end = new Date(now + millisPerDay);
            BigInteger serial = BigInteger.valueOf(1);

            X509V3CertificateGenerator x509cg = new X509V3CertificateGenerator();
            x509cg.setSubjectDN(subject);
            x509cg.setIssuerDN(issuer);
            x509cg.setNotBefore(start);
            x509cg.setNotAfter(end);
            x509cg.setPublicKey(publicKey);
            x509cg.setSignatureAlgorithm(signatureAlgorithm);
            x509cg.setSerialNumber(serial);
            if (keyUsage != 0) {
                x509cg.addExtension(X509Extensions.KeyUsage,
                                    true,
                                    new KeyUsage(keyUsage));
            }
            if (ca) {
                x509cg.addExtension(X509Extensions.BasicConstraints,
                                    true,
                                    new BasicConstraints(true));
            }
            PrivateKey signingKey = (caKey == null) ? privateKey : caKey;
            if (signingKey instanceof ECPrivateKey) {
                /*
                 * bouncycastle needs its own ECPrivateKey implementation
                 */
                KeyFactory kf = KeyFactory.getInstance(keyAlgorithm, "BC");
                PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(signingKey.getEncoded());
                signingKey = (PrivateKey) kf.generatePrivate(ks);
            }
            x509c = x509cg.generateX509Certificate(signingKey);
            if (StandardNames.IS_RI) {
                /*
                 * The RI can't handle the BC EC signature algorithm
                 * string of "ECDSA", since it expects "...WITHEC...",
                 * so convert from BC to RI X509Certificate
                 * implementation via bytes.
                 */
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                ByteArrayInputStream bais = new ByteArrayInputStream(x509c.getEncoded());
                Certificate c = cf.generateCertificate(bais);
                x509c = (X509Certificate) c;
            }
        }

        X509Certificate[] x509cc;
        if (privateAlias == null) {
            // don't need certificate chain
            x509cc = null;
        } else if (caCertChain == null) {
            x509cc = new X509Certificate[] { x509c };
        } else {
            x509cc = new X509Certificate[caCertChain.length+1];
            x509cc[0] = x509c;
            System.arraycopy(caCertChain, 0, x509cc, 1, caCertChain.length);
        }

        // 3.) put certificate and private key into the key store
        if (privateAlias != null) {
            keyStore.setKeyEntry(privateAlias, privateKey, keyPassword, x509cc);
        }
        if (publicAlias != null) {
            keyStore.setCertificateEntry(publicAlias, x509c);
        }
        return keyStore;
    }

    /**
     * Create an X509Principal with the given attributes
     */
    public static X509Principal localhost() {
        try {
            return x509Principal(InetAddress.getLocalHost().getCanonicalHostName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create an X509Principal with the given attributes
     */
    public static X509Principal x509Principal(String commonName) {
        Hashtable attributes = new Hashtable();
        attributes.put(X509Principal.CN, commonName);
        return new X509Principal(attributes);
    }

    /**
     * Return the only private key in a TestKeyStore for the given
     * algorithms. Throws IllegalStateException if there are are more
     * or less than one.
     */
    public PrivateKeyEntry getPrivateKey(String keyAlgorithm, String signatureAlgorithm) {
        return privateKey(keyStore, keyPassword, keyAlgorithm, signatureAlgorithm);
    }

    /**
     * Return the only private key in a keystore for the given
     * algorithms. Throws IllegalStateException if there are are more
     * or less than one.
     */
    public static PrivateKeyEntry privateKey(KeyStore keyStore,
                                             char[] keyPassword,
                                             String keyAlgorithm,
                                             String signatureAlgorithm) {
        try {
            PrivateKeyEntry found = null;
            PasswordProtection password = new PasswordProtection(keyPassword);
            for (String alias: Collections.list(keyStore.aliases())) {
                if (!keyStore.entryInstanceOf(alias, PrivateKeyEntry.class)) {
                    continue;
                }
                PrivateKeyEntry privateKey = (PrivateKeyEntry) keyStore.getEntry(alias, password);
                if (!privateKey.getPrivateKey().getAlgorithm().equals(keyAlgorithm)) {
                    continue;
                }
                X509Certificate certificate = (X509Certificate) privateKey.getCertificate();
                if (!certificate.getSigAlgName().contains(signatureAlgorithm)) {
                    continue;
                }
                if (found != null) {
                    throw new IllegalStateException("keyStore has more than one private key for "
                                                    + " keyAlgorithm: " + keyAlgorithm
                                                    + " signatureAlgorithm: " + signatureAlgorithm
                                                    + "\nfirst: " + found.getPrivateKey()
                                                    + "\nsecond: " + privateKey.getPrivateKey() );
                }
                found = privateKey;
            }
            if (found == null) {
                throw new IllegalStateException("keyStore contained no private key for "
                                                + " keyAlgorithm: " + keyAlgorithm
                                                + " signatureAlgorithm: " + signatureAlgorithm);
            }
            return found;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the only self-signed root certificate in a TestKeyStore
     * for the given algorithm. Throws IllegalStateException if there
     * are are more or less than one.
     */
    public Certificate getRootCertificate(String algorithm) {
        return rootCertificate(keyStore, algorithm);
    }

    /**
     * Return the only self-signed root certificate in a keystore for
     * the given algorithm. Throws IllegalStateException if there are
     * are more or less than one.
     */
    public static Certificate rootCertificate(KeyStore keyStore,
                                              String algorithm) {
        try {
            Certificate found = null;
            for (String alias: Collections.list(keyStore.aliases())) {
                if (!keyStore.entryInstanceOf(alias, TrustedCertificateEntry.class)) {
                    continue;
                }
                TrustedCertificateEntry certificateEntry =
                        (TrustedCertificateEntry) keyStore.getEntry(alias, null);
                Certificate certificate = certificateEntry.getTrustedCertificate();
                if (!certificate.getPublicKey().getAlgorithm().equals(algorithm)) {
                    continue;
                }
                if (!(certificate instanceof X509Certificate)) {
                    continue;
                }
                X509Certificate x = (X509Certificate) certificate;
                if (!x.getIssuerDN().equals(x.getSubjectDN())) {
                    continue;
                }
                if (found != null) {
                    throw new IllegalStateException("keyStore has more than one root CA for "
                                                    + algorithm
                                                    + "\nfirst: " + found
                                                    + "\nsecond: " + certificate );
                }
                found = certificate;
            }
            if (found == null) {
                throw new IllegalStateException("keyStore contained no root CA for " + algorithm);
            }
            return found;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a client key store that only contains self-signed certificates but no private keys
     */
    public static KeyStore createClient(KeyStore caKeyStore) {
        try {
            KeyStore clientKeyStore = createKeyStore();
            copySelfSignedCertificates(clientKeyStore, caKeyStore);
            return clientKeyStore;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copy self-signed certifcates from one key store to another.
     * Returns true if successful, false if no match found.
     */
    public static boolean copySelfSignedCertificates(KeyStore dst, KeyStore src) throws Exception {
        boolean copied = false;
        for (String alias: Collections.list(src.aliases())) {
            if (!src.isCertificateEntry(alias)) {
                continue;
            }
            X509Certificate cert = (X509Certificate)src.getCertificate(alias);
            if (!cert.getSubjectDN().equals(cert.getIssuerDN())) {
                continue;
            }
            dst.setCertificateEntry(alias, cert);
            copied = true;
        }
        return copied;
    }

    /**
     * Copy named certifcates from one key store to another.
     * Returns true if successful, false if no match found.
     */
    public static boolean copyCertificate(Principal subject, KeyStore dst, KeyStore src)
            throws Exception {
        for (String alias: Collections.list(src.aliases())) {
            if (!src.isCertificateEntry(alias)) {
                continue;
            }
            X509Certificate cert = (X509Certificate)src.getCertificate(alias);
            if (!cert.getSubjectDN().equals(subject)) {
                continue;
            }
            dst.setCertificateEntry(alias, cert);
            return true;
        }
        return false;
    }

    /**
     * Dump a key store for debugging.
     */
    public static void dump(String context,
                            KeyStore keyStore,
                            char[] keyPassword) {
        try {
            PrintStream out = System.out;
            out.println("context=" + context);
            out.println("\tkeyStore=" + keyStore);
            out.println("\tkeyStore.type=" + keyStore.getType());
            out.println("\tkeyStore.provider=" + keyStore.getProvider());
            out.println("\tkeyPassword="
                        + ((keyPassword == null) ? null : new String(keyPassword)));
            out.println("\tsize=" + keyStore.size());
            for (String alias: Collections.list(keyStore.aliases())) {
                out.println("alias=" + alias);
                out.println("\tcreationDate=" + keyStore.getCreationDate(alias));
                if (keyStore.isCertificateEntry(alias)) {
                    out.println("\tcertificate:");
                    out.println("==========================================");
                    out.println(keyStore.getCertificate(alias));
                    out.println("==========================================");
                    continue;
                }
                if (keyStore.isKeyEntry(alias)) {
                    out.println("\tkey:");
                    out.println("==========================================");
                    String key;
                    try {
                        key = ("Key retreived using password\n"
                               + keyStore.getKey(alias, keyPassword));
                    } catch (UnrecoverableKeyException e1) {
                        try {
                            key = ("Key retreived without password\n"
                                   + keyStore.getKey(alias, null));
                        } catch (UnrecoverableKeyException e2) {
                            key = "Key could not be retreived";
                        }
                    }
                    out.println(key);
                    out.println("==========================================");
                    Certificate[] chain = keyStore.getCertificateChain(alias);
                    if (chain == null) {
                        out.println("No certificate chain associated with key");
                        out.println("==========================================");
                    } else {
                        for (int i = 0; i < chain.length; i++) {
                            out.println("Certificate chain element #" + i);
                            out.println(chain[i]);
                            out.println("==========================================");
                        }
                    }
                    continue;
                }
                out.println("\tunknown entry type");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertChainLength(Object[] chain) {
        /*
         * Note chain is Object[] to support both
         * java.security.cert.X509Certificate and
         * javax.security.cert.X509Certificate
         */
        assertEquals(3, chain.length);
    }
}
