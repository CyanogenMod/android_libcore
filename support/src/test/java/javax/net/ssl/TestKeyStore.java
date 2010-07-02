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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Hashtable;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;

/**
 * TestKeyStore is a convenience class for other tests that
 * want a canned KeyStore with a variety of key pairs.
 *
 * Creating a key store is relatively slow, so a singleton instance is
 * accessible via TestKeyStore.get().
 */
public final class TestKeyStore {

    public final KeyStore keyStore;
    public final char[] keyStorePassword;

    private TestKeyStore(KeyStore keyStore,
                         char[] keyStorePassword) {
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
    }

    private static final TestKeyStore SINGLETON = create(new String[] { "RSA" } );

    /**
     * Return a keystore with an RSA keypair
     */
    public static TestKeyStore get() {
        return SINGLETON;
    }

    /**
     * Create a new KeyStore containing the requested key types.
     * Since key generation can be expensive, most tests should reuse
     * the RSA-only singleton instance returned by TestKeyStore.get
     */
    public static TestKeyStore create(String[] keyAlgorithms) {
        try {
            char[] keyStorePassword = null;
            KeyStore keyStore = createKeyStore();
            for (String keyAlgorithm : keyAlgorithms) {
                String publicAlias  = "public-"  + keyAlgorithm;
                String privateAlias = "private-" + keyAlgorithm;
                createKeys(keyStore, keyStorePassword, keyAlgorithm, publicAlias, privateAlias);
            }
            return new TestKeyStore(keyStore, keyStorePassword);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create an empty BKS KeyStore
     *
     * The KeyStore is optionally password protected by the
     * keyStorePassword argument, which can be null if a password is
     * not desired.
     */
    public static KeyStore createKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("BKS");
        keyStore.load(null, null);
        return keyStore;
    }

    /**
     * Add newly generated keys of a given key type to an existing
     * KeyStore. The PrivateKey will be stored under the specified
     * private alias name and a X509Certificate based on the matching
     * PublicKey stored under the given public alias name.
     *
     * The private key will have a certificate chain including the
     * certificate stored under the alias name privateAlias. The
     * certificate will be signed by the private key. The certificate
     * Subject and Issuer Common-Name will be the local host's
     * canonical hostname. The certificate will be valid for one day
     * before and one day after the time of creation.
     *
     * Based on:
     * org.bouncycastle.jce.provider.test.SigTest
     * org.bouncycastle.jce.provider.test.CertTest
     */
    public static KeyStore createKeys(KeyStore keyStore,
                                      char[] keyStorePassword,
                                      String keyAlgorithm,
                                      String publicAlias,
                                      String privateAlias) throws Exception {
        PrivateKey privateKey;
        X509Certificate x509c;
        if (publicAlias == null && privateAlias == null) {
            // don't want anything apparently
            privateKey = null;
            x509c = null;
        } else {
            // 1.) we make the keys
            int keysize = 1024;
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(keyAlgorithm);
            kpg.initialize(keysize, new SecureRandom());
            KeyPair kp = kpg.generateKeyPair();
            privateKey = (PrivateKey)kp.getPrivate();
            PublicKey publicKey  = (PublicKey)kp.getPublic();
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
            x509cg.setSignatureAlgorithm("sha1With" + keyAlgorithm);
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

        // 3.) put certificate and private key into the key store
        if (privateAlias != null) {
            keyStore.setKeyEntry(privateAlias, privateKey, keyStorePassword, x509cc);
        }
        if (publicAlias != null) {
            keyStore.setCertificateEntry(publicAlias, x509c);
        }
        return keyStore;
    }
}
