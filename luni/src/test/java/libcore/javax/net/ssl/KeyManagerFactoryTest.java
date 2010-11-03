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

package libcore.javax.net.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore.Builder;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Set;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyStoreBuilderParameters;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;
import junit.framework.TestCase;
import libcore.java.security.StandardNames;
import libcore.java.security.TestKeyStore;

public class KeyManagerFactoryTest extends TestCase {

    // note the rare usage of DSA keys here in addition to RSA
    private static final TestKeyStore TEST_KEY_STORE
            = TestKeyStore.create(new String[] { "RSA", "DSA" },
                                  null,
                                  null,
                                  "rsa-dsa",
                                  TestKeyStore.localhost(),
                                  true,
                                  null);

    public void test_KeyManagerFactory_getDefaultAlgorithm() throws Exception {
        String algorithm = KeyManagerFactory.getDefaultAlgorithm();
        assertEquals(StandardNames.KEY_MANAGER_FACTORY_DEFAULT, algorithm);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        test_KeyManagerFactory(kmf, false);
    }

    private static class UseslessManagerFactoryParameters implements ManagerFactoryParameters {}

    private void test_KeyManagerFactory(KeyManagerFactory kmf,
                                        boolean supportsManagerFactoryParameters) throws Exception {
        assertNotNull(kmf);
        assertNotNull(kmf.getAlgorithm());
        assertNotNull(kmf.getProvider());

        // before init
        try {
            kmf.getKeyManagers();
            fail();
        } catch (IllegalStateException expected) {
        }

        // init with null ManagerFactoryParameters
        try {
            kmf.init(null);
            fail();
        } catch (InvalidAlgorithmParameterException expected) {
        }

        // init with useless ManagerFactoryParameters
        try {
            kmf.init(new UseslessManagerFactoryParameters());
            fail();
        } catch (InvalidAlgorithmParameterException expected) {
        }

        // init with KeyStoreBuilderParameters ManagerFactoryParameters
        PasswordProtection pp = new PasswordProtection(TEST_KEY_STORE.storePassword);
        Builder builder = Builder.newInstance(TEST_KEY_STORE.keyStore, pp);
        KeyStoreBuilderParameters ksbp = new KeyStoreBuilderParameters(builder);
        if (supportsManagerFactoryParameters) {
            kmf.init(ksbp);
            test_KeyManagerFactory_getKeyManagers(kmf);
        } else {
            try {
                kmf.init(ksbp);
                fail();
            } catch (InvalidAlgorithmParameterException expected) {
            }
        }

        // init with null for default behavior
        kmf.init(null, null);
        test_KeyManagerFactory_getKeyManagers(kmf);

        // init with specific key store and password
        kmf.init(TEST_KEY_STORE.keyStore, TEST_KEY_STORE.storePassword);
        test_KeyManagerFactory_getKeyManagers(kmf);
    }

    private void test_KeyManagerFactory_getKeyManagers(KeyManagerFactory kmf) {
        KeyManager[] keyManagers = kmf.getKeyManagers();
        assertNotNull(keyManagers);
        assertTrue(keyManagers.length > 0);
        for (KeyManager keyManager : keyManagers) {
            assertNotNull(keyManager);
            if (keyManager instanceof X509KeyManager) {
                test_X509KeyManager((X509KeyManager) keyManager);
            }
        }
    }

    String[] KEY_TYPES
            = StandardNames.KEY_TYPES.toArray(new String[StandardNames.KEY_TYPES.size()]);

    private void test_X509KeyManager(X509KeyManager km) {
        test_X509KeyManager_alias(km, km.chooseClientAlias(KEY_TYPES, null, null), null);
        for (String keyType : KEY_TYPES) {
            test_X509KeyManager_alias(km, km.chooseServerAlias(keyType, null, null), keyType);
        }
        for (String keyType : KEY_TYPES) {
            String[] aliases = km.getServerAliases(keyType, null);
            if (aliases == null) {
                continue;
            }
            for (String alias : aliases) {
                test_X509KeyManager_alias(km, alias, keyType);
            }
        }

        if (km instanceof X509ExtendedKeyManager) {
            test_X509ExtendedKeyManager((X509ExtendedKeyManager) km);
        }
    }

    private void test_X509ExtendedKeyManager(X509ExtendedKeyManager km) {
        test_X509KeyManager_alias(km,
                                  km.chooseEngineClientAlias(KEY_TYPES, null, null),
                                  null);
        for (String keyType : KEY_TYPES) {
            test_X509KeyManager_alias(km, km.chooseEngineServerAlias(keyType, null, null), keyType);
        }
    }

    private void test_X509KeyManager_alias(X509KeyManager km, String alias, String keyType) {
        if (alias == null) {
            assertNull(km.getCertificateChain(alias));
            assertNull(km.getPrivateKey(alias));
            return;
        }

        X509Certificate[] certificateChain = km.getCertificateChain(alias);
        PrivateKey privateKey = km.getPrivateKey(alias);

        if (keyType == null) {
            keyType = privateKey.getAlgorithm();
        } else {
            assertEquals(keyType, certificateChain[0].getPublicKey().getAlgorithm());
            assertEquals(keyType, privateKey.getAlgorithm());
        }

        PrivateKeyEntry privateKeyEntry = TEST_KEY_STORE.getPrivateKey(keyType);
        assertEquals(Arrays.asList(privateKeyEntry.getCertificateChain()),
                     Arrays.asList(certificateChain));
        assertEquals(privateKeyEntry.getPrivateKey(), privateKey);
    }

    public void test_KeyManagerFactory_getInstance() throws Exception {
        Provider[] providers = Security.getProviders();
        for (Provider provider : providers) {
            Set<Provider.Service> services = provider.getServices();
            for (Provider.Service service : services) {
                String type = service.getType();
                if (!type.equals("KeyManagerFactory")) {
                    continue;
                }
                String algorithm = service.getAlgorithm();
                boolean supportsManagerFactoryParameters = algorithm.equals("NewSunX509");
                {
                    KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
                    assertEquals(algorithm, kmf.getAlgorithm());
                    test_KeyManagerFactory(kmf, supportsManagerFactoryParameters);
                }

                {
                    KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm,
                                                                          provider);
                    assertEquals(algorithm, kmf.getAlgorithm());
                    assertEquals(provider, kmf.getProvider());
                    test_KeyManagerFactory(kmf, supportsManagerFactoryParameters);
                }

                {
                    KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm,
                                                                          provider.getName());
                    assertEquals(algorithm, kmf.getAlgorithm());
                    assertEquals(provider, kmf.getProvider());
                    test_KeyManagerFactory(kmf, supportsManagerFactoryParameters);
                }
            }
        }
    }
}
